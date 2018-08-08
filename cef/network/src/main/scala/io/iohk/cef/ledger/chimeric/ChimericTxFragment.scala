package io.iohk.cef.ledger.chimeric

sealed trait ChimericTxFragment

sealed trait TxInput extends ChimericTxFragment {
  def value: Value
}

sealed trait TxOutput extends ChimericTxFragment {
  def value: Value
}

sealed trait TxAction extends ChimericTxFragment

case class Withdrawal(address: Address, value: Value, nonce: Int) extends TxInput
case class Mint(value: Value) extends TxInput
case class Input(txOutRef: TxOutRef, value: Value) extends TxInput
case class Fee(value: Value) extends TxOutput
//TODO: Add the identity concept here
case class Output(value: Value) extends TxOutput
case class Deposit(address: Address, value: Value) extends TxOutput
case class CreateCurrency(currency: Currency) extends TxAction
