package io.iohk.cef.core
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{NetworkComponent, NodeInfo}
import io.iohk.cef.txpool.TxPool
import io.iohk.cef.utils.ForExpressionsEnabler

class NodeCore[F[+_]](implicit fexp: ForExpressionsEnabler[F]) {

  def receiveTransaction(txEnvelope: TxEnvelope): F[Either[ApplicationError, Unit]] = {
    val disseminationF = networkComponent.disseminate(txEnvelope)
    if(!txEnvelope.destinationDescriptor(me)) {
      disseminationF
    } else if(!consensusMap.contains(txEnvelope.ledgerId)) {
      val result: Either[ApplicationError, Unit] = Left(MissingCapabilitiesForTx(me, txEnvelope))
      pure(result)
    } else for {
      txProcess <- fexp.enableForExp(consensusMap(txEnvelope.ledgerId)._1.process(txEnvelope.transaction))
      dissemination <- fexp.enableForExp(disseminationF)
    } yield {
      dissemination.flatMap(_ => txProcess)
    }
  }

  //FIXME: Doesn't belong here
  protected def pure[A](a: A): F[A] = ???

  //FIXME: Think about a better handling of these dependencies
  protected def consensusMap: Map[LedgerId, (TxPool[F, _], Consensus)] = ???

  /**
    * Represents current node
    * @return
    */
  protected def me: NodeInfo = ???

  protected def networkComponent: NetworkComponent[F] = ???
}
