package io.iohk.cef.consensus
import akka.actor.ActorRef
import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}

import scala.concurrent.Future

trait Consensus[State, Tx <: Transaction[State]] {
  def ledgerId: LedgerId

  //If Consensus is a leader, it will ask the txPool for the next block
  def txPoolRef: ActorRef

  def process[Header <: BlockHeader](block: Block[State, Header, Tx]): Future[Either[ConsensusError, Unit]]
}
