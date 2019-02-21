package io.iohk.cef.transactionservice
import io.iohk.cef.ledger.LedgerId
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.query.{LedgerQuery, LedgerQueryService}
import io.iohk.cef.ledger.{Block, Transaction}
import io.iohk.network.{Envelope, Network, NodeId}
import io.iohk.cef.transactionpool.TransactionPoolInterface

import scala.concurrent.{ExecutionContext, Future}
import io.iohk.codecs.nio._

class NodeTransactionServiceImpl[State, Tx <: Transaction[State], Q <: LedgerQuery[State]](
    consensusMap: Map[
      LedgerId,
      (TransactionPoolInterface[State, Tx], Consensus[State, Tx], LedgerQueryService[State, Q])
    ],
    txNetwork: Network[Envelope[Tx]],
    blockNetwork: Network[Envelope[Block[State, Tx]]],
    me: NodeId
)(
    implicit txSerializable: NioCodec[Envelope[Tx]],
    blockSerializable: NioCodec[Envelope[Block[State, Tx]]],
    executionContext: ExecutionContext
) extends NodeTransactionService[State, Tx, Q] {

  blockNetwork.messageStream.foreach(blEnvelope => processBlock(blEnvelope, Future.successful(Right(()))))
  txNetwork.messageStream.foreach(txEnvelope => processTransaction(txEnvelope, Future.successful(Right(()))))

  override def receiveTransaction(txEnvelope: Envelope[Tx]): Future[Either[ApplicationError, Unit]] = {
    require(supportedLedgerIds.contains(txEnvelope.containerId))
    processTransaction(txEnvelope, disseminate(txEnvelope, txNetwork))
  }

  override def receiveBlock(blEnvelope: Envelope[Block[State, Tx]]): Future[Either[ApplicationError, Unit]] = {
    require(supportedLedgerIds.contains(blEnvelope.containerId))
    processBlock(blEnvelope, disseminate(blEnvelope, blockNetwork))
  }

  override def supportedLedgerIds: Set[LedgerId] = consensusMap.keySet

  override def getQueryService(ledgerId: LedgerId): LedgerQueryService[State, Q] = {
    require(supportedLedgerIds.contains(ledgerId))
    consensusMap(ledgerId)._3
  }

  private def processTransaction(
      txEnvelope: Envelope[Tx],
      networkDissemination: Future[Either[ApplicationError, Unit]]
  ) = {
    process(txEnvelope, networkDissemination) { env =>
      val txPoolService = consensusMap(env.containerId)._1
      Future(txPoolService.processTransaction(txEnvelope.content))
    }
  }

  private def processBlock(
      blEnvelope: Envelope[Block[State, Tx]],
      networkDissemination: Future[Either[ApplicationError, Unit]]
  ) = {
    process(blEnvelope, networkDissemination)(env => consensusMap(env.containerId)._2.process(env.content))
  }

  private def disseminate[A](
      envelope: Envelope[A],
      network: Network[Envelope[A]]
  ): Future[Either[ApplicationError, Unit]] =
    Future(Right(network.disseminateMessage(envelope)))

  private def thisIsDestination[A](envelope: Envelope[A]): Boolean = envelope.destinationDescriptor(me)

  private def thisParticipatesInConsensus(ledgerId: LedgerId): Boolean = consensusMap.contains(ledgerId)

  private def process[T](txEnvelope: Envelope[T], networkDissemination: Future[Either[ApplicationError, Unit]])(
      submit: Envelope[T] => Future[Either[ApplicationError, Unit]]
  )(implicit byteStringSerializable: NioCodec[Envelope[T]]): Future[Either[ApplicationError, Unit]] = {
    if (!thisIsDestination(txEnvelope)) {
      networkDissemination
    } else if (!thisParticipatesInConsensus(txEnvelope.containerId)) {
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
