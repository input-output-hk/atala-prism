package io.iohk.cef.transactionpool

import java.time.Clock

import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger._
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.codecs.nio.NioCodec

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.concurrent.stm.{Ref, atomic}
import scala.reflect.runtime.universe.TypeTag

/**
  * Interface for generating block and processing transaction received from node transactionservice
  *
  * @param headerGenerator
  * @param maxBlockSize
  * @param ledgerStateStorage
  * @param defaultTransactionExpiration
  * @param timedQueueConstructor
  * @param proposedTransactionsChannel the channel provides the transactions that are proposed by
  *                                    users (from the REST API / UI) but aren't applied to the ledger state.
  * @param executionContext
  * @tparam State
  * @tparam Tx
  */
class TransactionPoolInterface[State: NioCodec: TypeTag, Tx <: Transaction[State]](
    headerGenerator: Seq[Transaction[State]] => BlockHeader,
    maxBlockSize: Int,
    ledgerStateStorage: LedgerStateStorage[State],
    defaultTransactionExpiration: Duration,
    timedQueueConstructor: () => TimedQueue[Tx],
    proposedTransactionsObservable: ProposedTransactionsObservable[Tx],
    appliedBlocksObservable: AppliedBlocksObservable[State, Tx]
)(implicit executionContext: ExecutionContext) {

  import monix.execution.Scheduler.Implicits.global

  val _ = proposedTransactionsObservable.foreach { tx =>
    processTransaction(tx)
  }

  appliedBlocksObservable.foreach { appliedBlock =>
    removeBlockTransactions(appliedBlock)
  }

  private type BlockType = Block[State, Tx]

  private val mutableTransactionPool: Ref[TransactionPool[State, Tx]] = Ref(initializePool())

  private def initializePool(): TransactionPool[State, Tx] = {
    new TransactionPool[State, Tx](
      timedQueueConstructor(),
      headerGenerator,
      maxBlockSize,
      ledgerStateStorage,
      defaultTransactionExpiration
    )
  }

  def generateBlock(): Either[ApplicationError, BlockType] = {
    val pool = mutableTransactionPool.single()
    val block = pool.generateBlock()
    val state = ledgerStateStorage.slice(block.partitionIds)
    block(state).map(_ => block)
  }

  private def processTransaction(transaction: Tx): Either[ApplicationError, Unit] = {
    atomic { implicit txn =>
      val pool = mutableTransactionPool()
      pool.processTransaction(transaction).map { newPool =>
        mutableTransactionPool() = newPool
        ()
      }
    }
  }

  private def removeBlockTransactions(block: Block[State, Tx]): Unit = {
    atomic { implicit txn =>
      val pool = mutableTransactionPool.single()
      val newPool = pool.removeBlockTransactions(block)
      mutableTransactionPool() = newPool
    }
  }
}

object TransactionPoolInterface {
  def apply[State: NioCodec: TypeTag, Tx <: Transaction[State]](
      headerGenerator: Seq[Transaction[State]] => BlockHeader,
      maxBlockSize: Int,
      ledgerStateStorage: LedgerStateStorage[State],
      defaultTransactionExpiration: Duration,
      clock: Clock,
      proposedTransactionsObservable: ProposedTransactionsObservable[Tx],
      appliedBlocksObservable: AppliedBlocksObservable[State, Tx]
  )(implicit executionContext: ExecutionContext): TransactionPoolInterface[State, Tx] = {
    new TransactionPoolInterface(
      headerGenerator,
      maxBlockSize,
      ledgerStateStorage,
      defaultTransactionExpiration,
      () => new TimedQueue[Tx](clock),
      proposedTransactionsObservable,
      appliedBlocksObservable
    )
  }

  def apply[State: NioCodec: TypeTag, Tx <: Transaction[State]](
      headerGenerator: Seq[Transaction[State]] => BlockHeader,
      maxBlockSize: Int,
      ledgerStateStorage: LedgerStateStorage[State],
      defaultTransactionExpiration: Duration,
      proposedTransactionsObservable: ProposedTransactionsObservable[Tx],
      appliedBlocksObservable: AppliedBlocksObservable[State, Tx]
  )(implicit executionContext: ExecutionContext): TransactionPoolInterface[State, Tx] = {
    new TransactionPoolInterface(
      headerGenerator,
      maxBlockSize,
      ledgerStateStorage,
      defaultTransactionExpiration,
      () => new TimedQueue[Tx](),
      proposedTransactionsObservable,
      appliedBlocksObservable
    )
  }
}
