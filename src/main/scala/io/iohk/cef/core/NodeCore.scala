package io.iohk.cef.core
import io.iohk.cef.LedgerId
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.network.{Network, NodeId}
import io.iohk.cef.transactionpool.TransactionPoolFutureInterface

import scala.concurrent.{ExecutionContext, Future}

/**
  * A NodeCore orchestrates the interaction between the frontend, network, transaction pool and the consensus.
  * Important note: Currently it is assumed that the network is fully connected. If this assumption does not hold,
  * the NodeCore's dissemination will not reach all nodes.
  * @param consensusMap The ledgers and their respective Consensus that this node supports.
  * @param txNetwork Network in charge of disseminating transactions
  * @param blockNetwork Network in charge of disseminating blocks
  * @param me information about "me" (the node this core belongs to)
  * @param txSerializable
  * @param blockSerializable
  * @param timeout
  * @param executionContext
  * @tparam State
  * @tparam Header
  */
class NodeCore[State, Header <: BlockHeader, Tx <: Transaction[State]](
    consensusMap: Map[LedgerId, (TransactionPoolFutureInterface[State, Header, Tx], Consensus[State, Header, Tx])],
    txNetwork: Network[Envelope[Tx]],
    blockNetwork: Network[Envelope[Block[State, Header, Tx]]],
    me: NodeId)(
    implicit txSerializable: ByteStringSerializable[Envelope[Tx]],
    blockSerializable: ByteStringSerializable[Envelope[Block[State, Header, Tx]]],
    executionContext: ExecutionContext) {

  blockNetwork.messageStream.foreach(blEnvelope => processBlock(blEnvelope, Future.successful(Right(()))))
  txNetwork.messageStream.foreach(txEnvelope => processTransaction(txEnvelope, Future.successful(Right(()))))

  def receiveTransaction(txEnvelope: Envelope[Tx]): Future[Either[ApplicationError, Unit]] = {
    processTransaction(txEnvelope, disseminate(txEnvelope, txNetwork))
  }

  def receiveBlock(blEnvelope: Envelope[Block[State, Header, Tx]]): Future[Either[ApplicationError, Unit]] = {
    processBlock(blEnvelope, disseminate(blEnvelope, blockNetwork))
  }

  private def processTransaction(
      txEnvelope: Envelope[Tx],
      networkDissemination: Future[Either[ApplicationError, Unit]]) = {
    process(txEnvelope, networkDissemination) { env =>
      val txPoolService = consensusMap(env.ledgerId)._1
      txPoolService.processTransaction(txEnvelope.content)
    }
  }

  private def processBlock(
      blEnvelope: Envelope[Block[State, Header, Tx]],
      networkDissemination: Future[Either[ApplicationError, Unit]]) = {
    process(blEnvelope, networkDissemination)(env => consensusMap(env.ledgerId)._2.process(env.content))
  }

  private def disseminate[A](
      envelope: Envelope[A],
      network: Network[Envelope[A]]): Future[Either[ApplicationError, Unit]] =
    Future(Right(network.disseminateMessage(envelope)))

  private def thisIsDestination[A](envelope: Envelope[A]): Boolean = envelope.destinationDescriptor(me)

  private def thisParticipatesInConsensus(ledgerId: LedgerId): Boolean = consensusMap.contains(ledgerId)

  private def process[T](txEnvelope: Envelope[T], networkDissemination: Future[Either[ApplicationError, Unit]])(
      submit: Envelope[T] => Future[Either[ApplicationError, Unit]])(
      implicit byteStringSerializable: ByteStringSerializable[Envelope[T]]): Future[Either[ApplicationError, Unit]] = {
    if (!thisIsDestination(txEnvelope)) {
      networkDissemination
    } else if (!thisParticipatesInConsensus(txEnvelope.ledgerId)) {
      for {
        _ <- networkDissemination
      } yield {
        Left(MissingCapabilitiesForTx(me, txEnvelope))
      }
    } else
      for {
        txProcess <- submit(txEnvelope)
        dissemination <- networkDissemination
      } yield {
        dissemination.flatMap(_ => txProcess)
      }
  }
}
