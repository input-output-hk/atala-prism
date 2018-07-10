package io.iohk.cef.ledger.storage.db

object Schema {

  val SchemaName = "cef"

  val IdentityLedgerTableName = s"${SchemaName}.identity_ledger"
  val IdentityBlockTableName = s"${SchemaName}.identity_ledger_block"
  val IdentityTransactionTableName = s"${SchemaName}.identity_ledger_transaction"
  val IdentityStateTableName = s"${SchemaName}.identity_ledger_state"
}
