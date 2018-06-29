package io.iohk.cef.ledger

trait Ledger {
  val Identifier: Int

  def add(ledgerItem: LedgerItem)
}
