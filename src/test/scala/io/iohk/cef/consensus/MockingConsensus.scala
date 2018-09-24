package io.iohk.cef.consensus
import io.iohk.cef.ledger.{BlockHeader, Transaction}
import org.scalatest.mockito.MockitoSugar

trait MockingConsensus[State, Header <: BlockHeader, Tx <: Transaction[State]] {
  self: MockitoSugar =>

  def mockConsensus: Consensus[State, Header, Tx] = mock[Consensus[State, Header, Tx]]

}
