package io.iohk.cef.transactionpool
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.codecs.nio.NioCodec

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.concurrent.stm.{Ref, atomic}
import java.time.Clock
import scala.reflect.runtime.universe.TypeTag

/**
  * Interface for non actor model
  *
  */
/**
  * Interface for generating block and processing transaction received from node transactionservice
  * @param headerGenerator
  * @param maxBlockSize
  * @param ledgerStateStorage
  * @param defaultTransactionExpiration
  * @param timedQueueConstructor
  * @param timeout
  * @param blockByteSizeable
  * @param executionContext
  * @tparam State
  * @tparam Header
  * @tparam Tx
  */
class TransactionPoolInterface[State: NioCodec: TypeTag, Tx <: Transaction[State]](
    headerGenerator: Seq[Transaction[State]] => BlockHeader,
    maxBlockSize: Int,
    ledgerStateStorage: LedgerStateStorage[State],
    defaultTransactionExpiration: Duration,
    timedQueueConstructor: () => TimedQueue[Tx]
)(implicit executionContext: ExecutionContext) {

  type BlockType = Block[State, Tx]

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

  def processTransaction(transaction: Tx): Either[ApplicationError, Unit] = {
    atomic { implicit txn =>
      val pool = mutableTransactionPool()
      pool.processTransaction(transaction).map { newPool =>
        mutableTransactionPool() = newPool
        ()
      }
    }
  }

  def removeBlockTransactions(block: Block[State, Tx]): Either[ApplicationError, Unit] = {
    atomic { implicit txn =>
      val pool = mutableTransactionPool.single()
      val newPool = pool.removeBlockTransactions(block)
      mutableTransactionPool() = newPool
      Right(())
    }
  }
}

object TransactionPoolInterface {
  def apply[State: NioCodec: TypeTag, Tx <: Transaction[State]](
      headerGenerator: Seq[Transaction[State]] => BlockHeader,
      maxBlockSize: Int,
      ledgerStateStorage: LedgerStateStorage[State],
      defaultTransactionExpiration: Duration,
      clock: Clock
  )(implicit executionContext: ExecutionContext): TransactionPoolInterface[State, Tx] =
    new TransactionPoolInterface(
      headerGenerator,
      maxBlockSize,
      ledgerStateStorage,
      defaultTransactionExpiration,
      () => new TimedQueue[Tx](clock)
    )
  def apply[State: NioCodec: TypeTag, Tx <: Transaction[State]](
      headerGenerator: Seq[Transaction[State]] => BlockHeader,
      maxBlockSize: Int,
      ledgerStateStorage: LedgerStateStorage[State],
      defaultTransactionExpiration: Duration
  )(implicit executionContext: ExecutionContext): TransactionPoolInterface[State, Tx] =
    new TransactionPoolInterface(
      headerGenerator,
      maxBlockSize,
      ledgerStateStorage,
      defaultTransactionExpiration,
      () => new TimedQueue[Tx]()
    )
}
