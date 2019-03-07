package io.iohk.cef.ledger

case class UnsupportedLedgerException(ledgerId: LedgerId) extends IllegalArgumentException {

  override def toString: LedgerId = s"The given ledger is not supported: $ledgerId"
}
