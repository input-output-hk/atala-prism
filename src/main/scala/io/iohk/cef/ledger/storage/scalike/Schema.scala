package io.iohk.cef.ledger.storage.scalike

object Schema {

  val SchemaName = "cef"

  val LedgerTableName = s"${SchemaName}.ledger_block"
  val LedgerStateTableName = s"${SchemaName}.ledger_state_entry"
}
