package io.iohk.cef.transactionpool
import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import io.iohk.cef.consensus.{Consensus, ConsensusError}
import io.iohk.cef.ledger.{BlockHeader, Transaction}
import io.iohk.cef.transactionpool.BlockCreator.ConsensusResponse

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class BlockCreator[State, Header <: BlockHeader, Tx <: Transaction[State]](
    transactionPoolInterface: TransactionPoolActorModelInterface[State, Header, Tx],
    consensus: Consensus[State, Header, Tx],
    initialDelay: FiniteDuration,
    interval: FiniteDuration)(implicit executionContext: ExecutionContext)
    extends Actor
    with ActorLogging {

  context.system.scheduler.schedule(initialDelay, interval) {
    transactionPoolInterface.poolActor.tell(new transactionPoolInterface.GenerateBlock(), self)
  }

  override def receive: Receive = {
    case transactionPoolInterface.GenerateBlockResponse(Left(error)) =>
      log.error(s"Could not create block. Cause: ${error}")
    case transactionPoolInterface.GenerateBlockResponse(Right(block)) =>
      consensus.process(block).map(ConsensusResponse.apply) pipeTo self
    case ConsensusResponse(response) =>
      response match {
        case Left(error) =>
          log.error(s"Consensus could not process the block. Cause ${error}")
        case Right(_) =>
      }
  }
}

object BlockCreator {
  case class ConsensusResponse(response: Either[ConsensusError, Unit])
}
