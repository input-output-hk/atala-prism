package io.iohk.cef.transactionpool
import akka.actor.{Actor, ActorRef, Props}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.utils.ByteSizeable

import scala.collection.immutable.Queue

/**
  * A TransactionPoolHolder utilizes Akka to provide a [[TransactionPool]] with support for concurrency.
  * Subtyping is utilized to avoid type parameters in the Akka messages
  * @param actorCreator a constructor for the TransactionPool actor. By providing a constructor, selecting the actor's
  *                     parent is delegated to another layer.
  * @param headerGenerator a generator for the block header based on the block's transactions
  * @param maxTxSizeInBytes maximum size a block can have in bytes. Must be positive
  * @param blockByteSizeable type class that allows a block to be measured in bytes
  * @tparam State the ledger state type
  * @tparam Header the block header type
  */
class TransactionPoolActorHolder[State, Header <: BlockHeader](
  actorCreator: Props => ActorRef,
  headerGenerator: Seq[Transaction[State]] => Header,
  maxTxSizeInBytes: Int)(
  implicit blockByteSizeable: ByteSizeable[Block[State, Header, Transaction[State]]]) {

  type BlockType = Block[State, Header, Transaction[State]]

  val poolActor = actorCreator(Props(new TransactionPoolActor()))

  /**
    * A TransactionPoolActor provides a [[TransactionPool]] with support for concurrency.
    * @param blockByteSizeable type class that allows a block to be measured in bytes
    */
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
