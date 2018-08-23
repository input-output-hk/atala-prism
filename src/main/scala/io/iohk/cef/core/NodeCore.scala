package io.iohk.cef.core
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.network.{NetworkComponent, NodeInfo}
import io.iohk.cef.transactionpool.TransactionPoolService
import io.iohk.cef.utils.ForExpressionsEnabler

import scala.language.higherKinds

class NodeCore[F[+_], State, Header <: BlockHeader](
    implicit fexp: ForExpressionsEnabler[F],
    txSerializable: ByteStringSerializable[Envelope[Transaction[State]]],
    blockSerializable: ByteStringSerializable[Envelope[Block[State, Header, Transaction[State]]]]) {

  def receiveTransaction(txEnvelope: Envelope[Transaction[State]]): F[Either[ApplicationError, Unit]] = {
    process(txEnvelope)(env => {
      pure(consensusMap(env.ledgerId)._1.processTransaction(env.content))
    })
  }

  def receiveBlock(blEnvelope: Envelope[Block[State, Header, Transaction[State]]]): F[Either[ApplicationError, Unit]] = {
    process(blEnvelope)(env => consensusMap(env.ledgerId)._2.process(env.content))
  }

  //FIXME: Doesn't belong here
  protected def pure[A](a: A): F[A] = ???

  //FIXME: Think about a better handling of these dependencies
  protected def consensusMap: Map[LedgerId, (TransactionPoolService[State, Header], Consensus[F, State])] = ???

  /**
    * Represents current node
    * @return
    */
  protected def me: NodeInfo = ???

  protected def networkComponent: NetworkComponent[F, State] = ???

  private def thisIsDestination[A](envelope: Envelope[A]): Boolean = envelope.destinationDescriptor(me)

  private def thisParticipatesInConsensus(ledgerId: LedgerId): Boolean = consensusMap.contains(ledgerId)

  private def process[T](txEnvelope: Envelope[T])(submit: Envelope[T] => F[Either[ApplicationError, Unit]])(
    implicit byteStringSerializable: ByteStringSerializable[Envelope[T]]): F[Either[ApplicationError, Unit]] = {
    val disseminationF = networkComponent.disseminate(txEnvelope)
    if (!thisIsDestination(txEnvelope)) {
      disseminationF
    } else if (!thisParticipatesInConsensus(txEnvelope.ledgerId)) {
      val result: Either[ApplicationError, Unit] = Left(MissingCapabilitiesForTx(me, txEnvelope))
      pure(result)
    } else
      for {
        txProcess <- fexp.enableForExp(submit(txEnvelope))
        dissemination <- fexp.enableForExp(disseminationF)
      } yield {
        dissemination.flatMap(_ => txProcess)
      }
  }
}
