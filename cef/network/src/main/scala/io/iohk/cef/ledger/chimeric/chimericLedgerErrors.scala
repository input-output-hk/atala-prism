package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.LedgerError

case class SpentOutputReferenced(txOutRef: TxOutRef) extends LedgerError {
  override def toString: String = s"Transaction referenced a spent output ${txOutRef}"
}

case class ValueNotPreserved(mint: Value, fee: Value, inputs: Value, outputs: Value) extends LedgerError {
  override def toString: String = s"Value is not preserved: ${mint + inputs} was not equal to ${fee + outputs}"
}

case class InsufficientBalance(address: Address, value: Value) extends LedgerError {
  override def toString(): String = s"Insufficient balance to withdraw from address ${address}. Requested ${value}"
}
