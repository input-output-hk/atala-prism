package io.iohk.cef.ledger

package object chimeric {
  type ChimericLedgerBlock = Block[ChimericStateResult, ChimericTx]
  type ChimericLedgerState = LedgerState[ChimericStateResult]
  type ChimericStateOrError = Either[LedgerError, ChimericLedgerState]

  type Currency = String
  type Quantity = BigDecimal
  type ChimericTxId = String
  type Address = String
}
