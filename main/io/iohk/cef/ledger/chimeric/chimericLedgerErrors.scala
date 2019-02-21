package io.iohk.cef.ledger.chimeric
package errors

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

case class InsufficientBalance(address: Address, requested: Value, found: Value) extends LedgerError {
  override def toString(): String =
    s"Insufficient balance to withdraw from address ${address}." +
      s"Requested ${requested}, but found ${found}"
}

case class CurrencyAlreadyExists(currency: Currency) extends LedgerError {
  override def toString: String = s"Currency ${currency} already exists"
}

case class CurrenciesDoNotExist(currencies: Seq[Currency], txFragment: ChimericTxFragment) extends LedgerError {
  override def toString: String =
    s"Tx Fragment ${txFragment} references one or more currencies that doesn't exist: $currencies"
}

case object MissingSignature extends LedgerError {
  override def toString: String =
    s"Tx fragments require signatures for all inputs and withdrawals."
}

case object InvalidSignature extends LedgerError {
  override def toString: String =
    s"The transaction contained an invalid signature."
}

case class InvalidNonce(expected: Int, actual: Int) extends LedgerError {
  override def toString: String = s"An invalid nonce has been received, expected = $expected, actual = $actual"
}

//Intrinsic errors
case class ValueNegative(value: Value)
    extends IllegalArgumentException(s"Value provided must be positive for all currencies ($value).")

case class ValueNotPreserved(totalValue: Value, fragments: Seq[ChimericTxFragment])
    extends IllegalArgumentException(
      s"Value is not preserved: ${totalValue} should be zero for all currencies. Fragments: $fragments"
    )
