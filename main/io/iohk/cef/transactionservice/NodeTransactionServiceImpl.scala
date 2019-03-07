package io.iohk.cef.transactionservice

import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.query.{LedgerQuery, LedgerQueryService}
import io.iohk.cef.ledger._
import io.iohk.codecs.nio._
import io.iohk.network.{Envelope, Network, NodeId}

import scala.concurrent.{ExecutionContext, Future}

/**
  * A NodeTransactionService orchestrates the interaction between the frontend, network, transaction pool and the consensus.
  * Important note: Currently it is assumed that the network is fully connected. If this assumption does not hold,
  * the NodeTransactionService's dissemination will not reach all nodes.
  *
  * @param consensusMap The ledgers and their respective Consensus that this node supports.
  * @param txNetwork Network in charge of disseminating transactions
  * @param blockNetwork Network in charge of disseminating blocks
  * @param me information about "me" (the node this transactionservice belongs to)
  */
class NodeTransactionServiceImpl[State, Tx <: Transaction[State], Q <: LedgerQuery[State]](
    consensusMap: LedgerServicesMap[State, Tx, Q],
    txNetwork: Network[Envelope[Tx]],
    blockNetwork: Network[Envelope[Block[State, Tx]]],
    me: NodeId
)(
    implicit txSerializable: NioCodec[Envelope[Tx]],
    blockSerializable: NioCodec[Envelope[Block[State, Tx]]],
    executionContext: ExecutionContext
) extends NodeTransactionService[State, Tx, Q] {

  blockNetwork.messageStream.foreach(blEnvelope => processBlock(blEnvelope, Future.successful(Right(()))))

  // FIXME: When scalanet exposes the monix stream, mere it with the consensusMap(env.containerId).transactionChannel
  // receives tx from other nodes, they replicate the tx pool
  txNetwork.messageStream.foreach(txEnvelope => processTransaction(txEnvelope, Future.successful(Right(()))))

  // receives tx from users (like a UI)
  override def receiveTransaction(txEnvelope: Envelope[Tx]): Future[Either[ApplicationError, Unit]] = {
    validateSupportedLedger(txEnvelope.containerId)
    processTransaction(txEnvelope, disseminate(txEnvelope, txNetwork))
  }

  override def receiveBlock(blEnvelope: Envelope[Block[State, Tx]]): Future[Either[ApplicationError, Unit]] = {
    validateSupportedLedger(blEnvelope.containerId)
    processBlock(blEnvelope, disseminate(blEnvelope, blockNetwork))
  }

  override def supportedLedgerIds: Set[LedgerId] = consensusMap.keySet

  override def getQueryService(ledgerId: LedgerId): LedgerQueryService[State, Q] = {
    validateSupportedLedger(ledgerId)
    consensusMap(ledgerId).ledgerQueryService
  }

  private def processTransaction(
      txEnvelope: Envelope[Tx],
      networkDissemination: Future[Either[ApplicationError, Unit]]
  ) = {
    process(txEnvelope, networkDissemination) { env =>
      val channel = consensusMap(env.containerId).proposedTransactionsObserver
      val _ = channel.onNext(txEnvelope.content)
      Future.successful(Right(()))
    }
  }

  private def processBlock(
      blEnvelope: Envelope[Block[State, Tx]],
      networkDissemination: Future[Either[ApplicationError, Unit]]
  ) = {
    process(blEnvelope, networkDissemination) { env =>
      val _ = consensusMap(env.containerId).proposedBlocksObserver
        .onNext(env.content)

      Future.successful(Right(()))
    }
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

  private def validateSupportedLedger(ledgerId: LedgerId): Unit = {
    require(supportedLedgerIds contains ledgerId, throw UnsupportedLedgerException(ledgerId))
  }
}
