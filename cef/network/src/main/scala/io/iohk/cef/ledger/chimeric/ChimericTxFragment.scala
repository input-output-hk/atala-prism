package io.iohk.cef.ledger.chimeric

sealed trait ChimericTxFragment {
  def partitionIds: Set[String]
}

class Fee(value: Value) extends ChimericTxFragment {
  override def partitionIds: Set[String] = Set()
}
class Mint(value: Value) extends ChimericTxFragment {
  override def partitionIds: Set[String] = Set()
}
class Create(currency: Currency) extends ChimericTxFragment {
  override def partitionIds: Set[String] = Set(currency)
}
class Output(address: Address, value: Value) extends ChimericTxFragment {
  override def partitionIds: Set[String] = Set(ChimericLedgerState.getPartitionKey(address))
}
class Input(txOutRef: TxOutRef) extends ChimericTxFragment {
  override def partitionIds: Set[String] = Set(ChimericLedgerState.getPartitionKey(txOutRef))
}
class Deposit(address: Address, value: Value) extends ChimericTxFragment {
  override def partitionIds: Set[String] = Set()
}
class Withdrawal(address: Address, value: Value, nonce: Int) extends ChimericTxFragment {
  override def partitionIds: Set[String] = Set(ChimericLedgerState.getPartitionKey(address))
}
class LedgerId(id: Int) extends ChimericTxFragment {
  override def partitionIds: Set[String] = Set()
}
