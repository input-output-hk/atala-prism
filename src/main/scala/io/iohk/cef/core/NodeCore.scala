package io.iohk.cef.core
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.network.{NetworkComponent, NodeInfo}
import io.iohk.cef.transactionpool.TransactionPoolService
import io.iohk.cef.utils.{ForExpressionsEnabler, HigherKindEnabler}
import akka.pattern.ask
import akka.util.Timeout

import scala.language.higherKinds

class NodeCore[F[+_], State, Header <: BlockHeader](
    consensusMap: Map[LedgerId, (TransactionPoolService[State, Header], Consensus[F, State])])(
    implicit fexp: ForExpressionsEnabler[F],
    higherKindEn: HigherKindEnabler[F],
    txSerializable: ByteStringSerializable[Envelope[Transaction[State]]],
    blockSerializable: ByteStringSerializable[Envelope[Block[State, Header, Transaction[State]]]],
    timeout: Timeout) {

  def receiveTransaction(txEnvelope: Envelope[Transaction[State]]): F[Either[ApplicationError, Unit]] = {
    process(txEnvelope)(env => {
      val txPoolService = consensusMap(env.ledgerId)._1
      val txProcess =
        (txPoolService.poolActor ? txPoolService.ProcessTransaction(txEnvelope.content)).mapTo[txPoolService.ProcessTransactionResponse]
      for {
        result <- fexp.enableForExp(higherKindEn.fromFuture(txProcess))
      } yield result.result
    })
  }

  def receiveBlock(blEnvelope: Envelope[Block[State, Header, Transaction[State]]]): F[Either[ApplicationError, Unit]] = {
    process(blEnvelope)(env => consensusMap(env.ledgerId)._2.process(env.content))
  }

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
      higherKindEn.wrap(result)
    } else
      for {
        txProcess <- fexp.enableForExp(submit(txEnvelope))
        dissemination <- fexp.enableForExp(disseminationF)
      } yield {
        dissemination.flatMap(_ => txProcess)
      }
  }
}
