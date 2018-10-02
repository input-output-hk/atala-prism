package io.iohk.cef.transactionpool
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.pipe
import io.iohk.cef.consensus.{Consensus, ConsensusError}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{BlockHeader, Transaction}
import io.iohk.cef.transactionpool.BlockCreator.{ConsensusResponse, Execute}

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
    self ! Execute(None)
  }

  override def receive: Receive = {
    case Execute(requester) =>
      transactionPoolInterface.poolActor ! (new transactionPoolInterface.GenerateBlock(requester))
    case transactionPoolInterface.GenerateBlockResponse(Left(error), requester) =>
      log.error(s"Could not create block. Cause: ${error}")
      requester.foreach(_ ! Left[ApplicationError, Unit](error))
    case transactionPoolInterface.GenerateBlockResponse(Right(block), requester) =>
      consensus.process(block).map(ConsensusResponse(_, requester)) pipeTo self
    case ConsensusResponse(response, requester) =>
      response match {
        case Left(error) =>
          log.error(s"Consensus could not process the block. Cause ${error}")
          requester.foreach(_ ! Left[ApplicationError, Unit](error))
        case Right(()) =>
          requester.foreach(_ ! Right[ApplicationError, Unit](()))
      }
  }
}

object BlockCreator {
  case class ConsensusResponse(response: Either[ConsensusError, Unit], requester: Option[ActorRef])
  case class Execute(requester: Option[ActorRef])
}
