package io.iohk.cef.transactionpool
import akka.actor.Actor
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.ledger.{BlockHeader, Transaction}
import io.iohk.cef.transactionpool.BlockCreator.CreateBlock

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class BlockCreator[State, Header <: BlockHeader, Tx <: Transaction[State]](
    transactionPoolInterface: TransactionPoolActorModelInterface[State, Header, Tx],
    consensus: Consensus[State, Header, Tx],
    initialDelay: FiniteDuration,
    interval: FiniteDuration)(implicit executionContext: ExecutionContext)
    extends Actor {

  context.system.scheduler.schedule(initialDelay, interval) {
    self ! CreateBlock()
  }

  override def receive: Receive = {
    case CreateBlock() =>
      transactionPoolInterface.poolActor ! new transactionPoolInterface.GenerateBlock()
  }
}

object BlockCreator {
  case class CreateBlock()
}
