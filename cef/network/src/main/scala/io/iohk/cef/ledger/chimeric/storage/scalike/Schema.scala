package io.iohk.cef.ledger.chimeric.storage.scalike

object Schema {

  val SchemaName = "cef"

  val LedgerStateTableName = s"$SchemaName.chimeric_ledger_state"
  val LedgerStateAddressTableName = s"$SchemaName.chimeric_ledger_state_address"
  val LedgerStateUtxoTableName = s"$SchemaName.chimeric_ledger_state_utxo"
  val LedgerStateCurrencyTableName = s"$SchemaName.chimeric_ledger_state_currency"
  val ValueTableName = s"$SchemaName.chimeric_value_entry"
}
