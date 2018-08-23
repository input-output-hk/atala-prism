package io.iohk.cef.transactionpool
import akka.actor.{Actor, ActorSystem, Props}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.utils.ByteSizeable

import scala.collection.immutable.Queue

class TransactionPoolService[State, Header <: BlockHeader](
  system: ActorSystem,
  headerGenerator: Seq[Transaction[State]] => Header,
  maxTxSizeInBytes: Int)(
  implicit blockByteSizeable: ByteSizeable[Block[State, Header, Transaction[State]]]) {

  type BlockType = Block[State, Header, Transaction[State]]

  val poolActor = system.actorOf(Props(new TransactionPoolActor()))

  class TransactionPoolActor(implicit blockByteSizeable: ByteSizeable[BlockType])
      extends Actor {

    var pool = new TransactionPool[State, Header](Queue(), headerGenerator, maxTxSizeInBytes)

    override def receive: Receive = {
      case GenerateBlock()        => sender() ! GenerateBlockResponse(generateBlock())
      case ProcessTransaction(tx) => sender() ! ProcessTransactionResponse(processTransaction(tx))
      case RemoveBlockTransactions(block) =>
        sender() ! RemoveBlockTransactionsResponse(removeBlockTransactions(block))
    }

    private def generateBlock(): Either[ApplicationError, BlockType] = {
      val (newPool, block) = pool.generateBlock()
      pool = newPool
      Right(block)
    }

    private def processTransaction(transaction: Transaction[State]): Either[ApplicationError, Unit] = {
      val newPool = pool.processTransaction(transaction)
      pool = newPool
      Right(())
    }

    private def removeBlockTransactions(
        block: Block[State, Header, Transaction[State]]): Either[ApplicationError, Unit] = {
      val newPool = pool.removeBlockTransactions(block)
      pool = newPool
      Right(())
    }
  }

  case class GenerateBlock()
  case class ProcessTransaction(tx: Transaction[State])
  case class RemoveBlockTransactions(block: Block[State, Header, Transaction[State]])

  case class GenerateBlockResponse(result: Either[ApplicationError, Block[State, Header, Transaction[State]]])
  case class ProcessTransactionResponse(result: Either[ApplicationError, Unit])
  case class RemoveBlockTransactionsResponse(result: Either[ApplicationError, Unit])
}
