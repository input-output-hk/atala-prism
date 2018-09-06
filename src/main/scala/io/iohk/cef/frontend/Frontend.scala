package io.iohk.cef.frontend
import io.iohk.cef.LedgerId
import io.iohk.cef.core.{Envelope, NodeCore}
import io.iohk.cef.ledger.{BlockHeader, Transaction}

class Frontend[State, Header <: BlockHeader, Tx <: Transaction[State]](nodeCore: NodeCore[State, Header, Tx]) {

  def processTx(transaction: Tx, ledgerId: LedgerId): Unit = {
    nodeCore.receiveTransaction(Envelope(transaction, ledgerId, _ => true))
  }
}
