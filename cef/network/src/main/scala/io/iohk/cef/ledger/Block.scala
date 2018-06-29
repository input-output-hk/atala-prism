package io.iohk.cef.ledger

case class Block(height: Int, items: Seq[LedgerItem])
