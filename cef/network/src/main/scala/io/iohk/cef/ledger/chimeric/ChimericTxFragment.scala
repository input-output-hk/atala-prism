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
case class Mint(value: Value, address: Address) extends TxInput {
  override def partitionIds: Set[String] = Set()
}
case class Input(txOutRef: TxOutRef, value: Value) extends TxInput {
  override def partitionIds: Set[String] = Set(ChimericLedgerState.getUtxoPartitionId(txOutRef))
}
case class Fee(value: Value, address: Address) extends TxOutput {
  override def partitionIds: Set[String] = Set()
}
case class Output(value: Value) extends TxOutput {
  override def partitionIds: Set[String] = Set(ChimericLedgerState.getUtxoPartitionId(address))
}
case class Deposit(address: Address, value: Value) extends TxOutput {
  override def partitionIds: Set[String] = Set()
}
case class CreateCurrency(currency: Currency) extends TxAction {
  override def partitionIds: Set[String] = Set(ChimericLedgerState.getCurrencyPartitionId(currency))
}
