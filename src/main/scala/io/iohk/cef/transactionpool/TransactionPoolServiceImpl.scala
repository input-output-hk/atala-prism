package io.iohk.cef.transactionpool
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.Transaction

import scala.collection.immutable.Queue

class TransactionPoolServiceImpl[State, Header](headerGenerator: Seq[Transaction[State]] => Header,
                                                maxTxsPerBlock: Int) extends TransactionPoolService[State, Header] {

  //FIXME Concurrency model? Currently NOT thread safe. Maybe ConcurrentLinkedQueue?
  var pool = TransactionPool[State, Header](Queue(), headerGenerator, maxTxsPerBlock)

  def generateBlock(): Either[ApplicationError, BlockType] = {
    val (newPool, block) = pool.generateBlock()
    pool = newPool
    Right(block)
  }

  def processTransaction(transaction: Transaction[State]): Either[ApplicationError, Unit] = {
    val newPool = pool.processTransaction(transaction)
    pool = newPool
    Right(())
  }

  def removeBlockTransactions(block: BlockType): Either[ApplicationError, Unit] ={
    val newPool = pool.removeBlockTransactions(block)
    pool = newPool
    Right(())
  }
}
