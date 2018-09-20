package io.iohk.cef.consensus
import io.iohk.cef.ledger.Transaction
import org.scalatest.mockito.MockitoSugar

trait MockingConsensus[State, Tx <: Transaction[State]] {
  self: MockitoSugar =>

  def mockConsensus: Consensus[State, Tx] = mock[Consensus[State, Tx]]

}
