package io.iohk.cef.consensus
import akka.actor.ActorRef
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}

import scala.language.higherKinds

trait Consensus[F[_], State] {
  def ledgerId: Int

  //If Consensus is a leader, it will ask the txPool for the next block
  def txPoolRef: ActorRef

  def process[Header <: BlockHeader](block: Block[State, Header, Transaction[State]]): F[Either[ConsensusError, Unit]]
}
