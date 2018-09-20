package io.iohk.cef.ledger.chimeric.storage.scalike

object Schema {

  val SchemaName = "cef"

  val LedgerStateEntryTableName = s"$SchemaName.chimeric_ledger_state_entry"
  val LedgerStateAddressTableName = s"$SchemaName.chimeric_ledger_state_address"
  val LedgerStateNonceTableName = s"$SchemaName.chimeric_ledger_state_nonce"
  val LedgerStateUtxoTableName = s"$SchemaName.chimeric_ledger_state_utxo"
  val LedgerStateCurrencyTableName = s"$SchemaName.chimeric_ledger_state_currency"
  val ValueTableName = s"$SchemaName.chimeric_value_entry"
}
