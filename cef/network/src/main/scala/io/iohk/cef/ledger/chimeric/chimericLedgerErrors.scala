package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.LedgerError

case class UnspentOutputNotFound(txOutRef: TxOutRef) extends LedgerError {
  override def toString: String = s"Transaction referenced a unspent output that was not found ${txOutRef}"
}

case class UnspentOutputInvalidValue(txOutRef: TxOutRef, expected: Value, actual: Value) extends LedgerError {
  override def toString: String =
    s"UTXO ${txOutRef} was expected to have ${expected} balance but has instead ${actual}"
}

case class UnspentOutputAlreadyExists(txOutRef: TxOutRef) extends LedgerError {
  override def toString: String =
    s"UTXO ${txOutRef} already exists in the ledger state"
}

case class ValueNotPreserved(totalValue: Value) extends LedgerError {
  override def toString: String = s"Value is not preserved: ${totalValue} should be zero for all currencies"
}

case class InsufficientBalance(address: Address, value: Value) extends LedgerError {
  override def toString(): String = s"Insufficient balance to withdraw from address ${address}. Requested ${value}"
}

case class CurrencyAlreadyExists(currency: Currency) extends LedgerError {
  override def toString: String = s"Currency ${currency} already exists"
}
