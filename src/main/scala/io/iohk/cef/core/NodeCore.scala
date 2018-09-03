package io.iohk.cef.core
import akka.util.Timeout
import io.iohk.cef.LedgerId
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.network.{NetworkComponent, NodeId}
import io.iohk.cef.transactionpool.TransactionPoolFutureInterface

import scala.concurrent.{ExecutionContext, Future}

/**
  * A NodeCore orchestrates the interaction between the frontend, network, transaction pool and the consensus.
  * @param consensusMap The ledgers and their respective Consensus that this node supports.
  * @param networkComponent
  * @param me information about "me" (the node this core belongs to)
  * @param txSerializable
  * @param blockSerializable
  * @param timeout
  * @param executionContext
  * @tparam State
  * @tparam Header
  */
class NodeCore[State, Header <: BlockHeader, Tx <: Transaction[State]](
    val consensusMap: Map[LedgerId, (TransactionPoolFutureInterface[State, Header, Tx], Consensus[State, Tx])],
    val networkComponent: NetworkComponent[State],
    val me: NodeId)(
    implicit txSerializable: ByteStringSerializable[Tx],
    blockSerializable: ByteStringSerializable[Block[State, Header, Tx]],
    timeout: Timeout,
    executionContext: ExecutionContext) {

  def receiveTransaction(txEnvelope: Envelope[Tx]): Future[Either[ApplicationError, Unit]] = {
    process(txEnvelope)(env => {
      val txPoolService = consensusMap(env.ledgerId)._1
      txPoolService.processTransaction(txEnvelope.content)
    })
  }

  def receiveBlock(blEnvelope: Envelope[Block[State, Header, Tx]]): Future[Either[ApplicationError, Unit]] = {
    process(blEnvelope)(env => consensusMap(env.ledgerId)._2.process(env.content))
  }

  private def thisIsDestination[A](envelope: Envelope[A]): Boolean = envelope.destinationDescriptor(me)

  private def thisParticipatesInConsensus(ledgerId: LedgerId): Boolean = consensusMap.contains(ledgerId)

  private def process[T](txEnvelope: Envelope[T])(submit: Envelope[T] => Future[Either[ApplicationError, Unit]])(
      implicit byteStringSerializable: ByteStringSerializable[T]): Future[Either[ApplicationError, Unit]] = {
    val disseminationF = networkComponent.disseminate(txEnvelope)
    if (!thisIsDestination(txEnvelope)) {
      disseminationF
    } else if (!thisParticipatesInConsensus(txEnvelope.ledgerId)) {
      Future.successful(Left(MissingCapabilitiesForTx(me, txEnvelope)))
    } else
      for {
        txProcess <- submit(txEnvelope)
        dissemination <- disseminationF
      } yield {
        dissemination.flatMap(_ => txProcess)
      }
  }
}
