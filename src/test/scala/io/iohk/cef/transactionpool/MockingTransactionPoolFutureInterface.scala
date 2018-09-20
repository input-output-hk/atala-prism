package io.iohk.cef.transactionpool
import io.iohk.cef.ledger.{BlockHeader, Transaction}
import org.scalatest.mockito.MockitoSugar

trait MockingTransactionPoolFutureInterface[State, Header <: BlockHeader, Tx <: Transaction[State]] {
  self: MockitoSugar =>

  def mockTxPoolFutureInterface: TransactionPoolFutureInterface[State, Header, Tx] =
    mock[TransactionPoolFutureInterface[State, Header, Tx]]

}
