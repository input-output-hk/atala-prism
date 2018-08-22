package io.iohk.cef.core
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{Block, Transaction}
import io.iohk.cef.network.{NetworkComponent, NodeInfo}
import io.iohk.cef.transactionpool.TransactionPool
import io.iohk.cef.utils.ForExpressionsEnabler

class NodeCore[F[+_], State](implicit fexp: ForExpressionsEnabler[F]) {

  def receiveTransaction(txEnvelope: Envelope[Transaction[State]]): F[Either[ApplicationError, Unit]] = {
    val disseminationF = networkComponent.disseminateTransaction(txEnvelope)
    if(!thisIsDestination(txEnvelope)) {
      disseminationF
    } else if(!thisParticipatesInConsensus(txEnvelope.ledgerId)) {
      val result: Either[ApplicationError, Unit] = Left(MissingCapabilitiesForTx(me, txEnvelope))
      pure(result)
    } else for {
      txProcess <- fexp.enableForExp(consensusMap(txEnvelope.ledgerId)._1.process(txEnvelope.content))
      dissemination <- fexp.enableForExp(disseminationF)
    } yield {
      dissemination.flatMap(_ => txProcess)
    }
  }

  def receiveBlock[Header](blEnvelope: Envelope[Block[State, Header, Transaction[State]]]): F[Either[ApplicationError, Unit]] = {
    val disseminationF = networkComponent.disseminateBlock(blEnvelope)
    if(!thisIsDestination(blEnvelope)) {
      disseminationF
    } else if(!thisParticipatesInConsensus(blEnvelope.ledgerId)) {
      val result: Either[ApplicationError, Unit] = Left(MissingCapabilitiesForTx(me, blEnvelope))
      pure(result)
    } else for {
      blockProcess <- fexp.enableForExp(consensusMap(blEnvelope.ledgerId)._2.process(blEnvelope.content))
      dissemination <- fexp.enableForExp(disseminationF)
    } yield {
      dissemination.flatMap(_ => blockProcess)
    }
  }

  //FIXME: Doesn't belong here
  protected def pure[A](a: A): F[A] = ???

  //FIXME: Think about a better handling of these dependencies
  protected def consensusMap: Map[LedgerId, (TransactionPool[F, State], Consensus[F, State])] = ???

  /**
    * Represents current node
    * @return
    */
  protected def me: NodeInfo = ???

  protected def networkComponent: NetworkComponent[F, State] = ???

  private def thisIsDestination[A](envelope: Envelope[A]): Boolean = envelope.destinationDescriptor(me)

  private def thisParticipatesInConsensus(ledgerId: LedgerId): Boolean = consensusMap.contains(ledgerId)
}
