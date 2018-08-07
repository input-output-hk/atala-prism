package io.iohk.cef.ledger.chimeric

sealed trait ChimericTxFragment {
  def partitionIds: Set[String]
}

sealed trait TxInput extends ChimericTxFragment {
  def value: Value
}

sealed trait TxOutput extends ChimericTxFragment {
  def value: Value
}

sealed trait TxMetaData extends ChimericTxFragment

sealed trait TxAction extends ChimericTxFragment

case class Withdrawal(address: Address, value: Value, nonce: Int) extends TxInput {
  override def partitionIds: Set[String] = Set(ChimericLedgerState.getAddressPartitionId(address))
}
case class Mint(value: Value) extends TxInput {
  override def partitionIds: Set[String] = Set()
}
case class Input(txOutRef: TxOutRef, value: Value) extends TxInput {
  override def partitionIds: Set[String] = Set(ChimericLedgerState.getPartitionId(txOutRef))
}

case class Fee(value: Value) extends TxOutput {
  override def partitionIds: Set[String] = Set()
}
case class Output(address: Address, value: Value) extends TxOutput {
  override def partitionIds: Set[String] = Set(ChimericLedgerState.getAddressPartitionId(address))
}
case class Deposit(address: Address, value: Value) extends TxOutput {
  override def partitionIds: Set[String] = Set()
}

case class CreateCurrency(currency: Currency) extends TxAction {
  override def partitionIds: Set[String] = Set(ChimericLedgerState.getCurrencyPartitionId(currency))
}

case class LedgerId(id: Int) extends TxMetaData {
  override def partitionIds: Set[String] = Set()
}
