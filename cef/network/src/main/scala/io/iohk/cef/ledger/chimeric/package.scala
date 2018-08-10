package io.iohk.cef.ledger

package object chimeric {
  type ChimericLedgerBlock = Block[ChimericStateValue, ChimericBlockHeader, ChimericTx]
  type ChimericLedgerState = LedgerState[ChimericStateValue]

  type Currency = String
  type Quantity = BigDecimal
  type ChimericTxId = String
  type Address = String
}
