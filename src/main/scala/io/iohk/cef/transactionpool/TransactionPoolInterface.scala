package io.iohk.cef.transactionpool
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.utils.ByteSizeable

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.concurrent.stm.{Ref, atomic}

/**
  * Interface for non actor model
  *
  */
/**
  * Interface for generating block and processing transaction received from node core
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
class TransactionPoolInterface[State, Header <: BlockHeader, Tx <: Transaction[State]](
    headerGenerator: Seq[Transaction[State]] => Header,
    maxBlockSize: Int,
    ledgerStateStorage: LedgerStateStorage[State],
    defaultTransactionExpiration: Duration,
    timedQueueConstructor: () => TimedQueue[Tx])(
    implicit blockByteSizeable: ByteSizeable[Block[State, Header, Tx]],
    executionContext: ExecutionContext) {

  type BlockType = Block[State, Header, Tx]

  private val mutableTransactionPool: Ref[TransactionPool[State, Header, Tx]] = Ref(initializePool())

  private def initializePool(): TransactionPool[State, Header, Tx] = {
    new TransactionPool[State, Header, Tx](
      timedQueueConstructor(),
      headerGenerator,
      maxBlockSize,
      ledgerStateStorage,
      defaultTransactionExpiration)
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

  def removeBlockTransactions(block: Block[State, Header, Tx]): Either[ApplicationError, Unit] = {
    atomic { implicit txn =>
      val pool = mutableTransactionPool.single()
      val newPool = pool.removeBlockTransactions(block)
      mutableTransactionPool() = newPool
      Right(())
    }
  }
}
