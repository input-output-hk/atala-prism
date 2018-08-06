package io.iohk.cef.ledger.chimeric

case class ChimericLedgerState(currencies: Map[Currency, Create],
                               accountBalance: Map[Address, Value],
                               utxos: Set[TxOutRef],
                               totalMint: Value,
                               totalFees: Value)

object ChimericLedgerState {
  def getPartitionKey(address: Address): String = s"address=$address"
  def getPartitionKey(txOutRef: TxOutRef): String = s"txOutRef=${txOutRef.index}|${txOutRef.id}"
}
