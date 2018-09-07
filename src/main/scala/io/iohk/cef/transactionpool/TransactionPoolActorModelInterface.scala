package io.iohk.cef.transactionpool
import akka.actor.{Actor, ActorRef, Props}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{Block, BlockHeader, LedgerState, Transaction}
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
class TransactionPoolActorModelInterface[State, Header <: BlockHeader, Tx <: Transaction[State]](
    actorCreator: Props => ActorRef,
    headerGenerator: Seq[Transaction[State]] => Header,
    maxTxSizeInBytes: Int)(implicit blockByteSizeable: ByteSizeable[Block[State, Header, Tx]]) {

  type BlockType = Block[State, Header, Tx]

  private lazy val actor = actorCreator(Props(new TransactionPoolActor()))

  def poolActor: ActorRef = actor

  /**
    * A TransactionPoolActor provides a [[TransactionPool]] with support for concurrency.
    * @param blockByteSizeable type class that allows a block to be measured in bytes
    */
  class TransactionPoolActor(implicit blockByteSizeable: ByteSizeable[BlockType]) extends Actor {

    var pool = new TransactionPool[State, Header, Tx](Queue(), headerGenerator, maxTxSizeInBytes)

    override def receive: Receive = {
      case GenerateBlock(ls) => sender() ! GenerateBlockResponse(generateBlock(ls))
      case ProcessTransaction(tx) => sender() ! ProcessTransactionResponse(processTransaction(tx))
      case RemoveBlockTransactions(block) =>
        sender() ! RemoveBlockTransactionsResponse(removeBlockTransactions(block))
    }

    private def generateBlock(currentLedgerState: LedgerState[State]): Either[ApplicationError, BlockType] = {
      val (newPool, block) = pool.generateBlock()
      val result = block(currentLedgerState).map(_ => block)
      pool = newPool
      result
    }

    private def processTransaction(transaction: Tx): Either[ApplicationError, Unit] = {
      val newPool = pool.processTransaction(transaction)
      pool = newPool
      Right(())
    }

    private def removeBlockTransactions(block: Block[State, Header, Tx]): Either[ApplicationError, Unit] = {
      val newPool = pool.removeBlockTransactions(block)
      pool = newPool
      Right(())
    }
  }

  case class GenerateBlock(currentLedgerState: LedgerState[State])
  case class ProcessTransaction(tx: Tx)
  case class RemoveBlockTransactions(block: Block[State, Header, Tx])

  case class GenerateBlockResponse(result: Either[ApplicationError, Block[State, Header, Tx]])
  case class ProcessTransactionResponse(result: Either[ApplicationError, Unit])
  case class RemoveBlockTransactionsResponse(result: Either[ApplicationError, Unit])
}
