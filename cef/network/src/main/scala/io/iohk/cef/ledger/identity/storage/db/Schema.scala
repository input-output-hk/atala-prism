package io.iohk.cef.ledger.identity.storage.db

object Schema {

  val SchemaName = "cef"

  val IdentityBlockTableName = s"${SchemaName}.identity_ledger_block"
  val IdentityTransactionTableName = s"${SchemaName}.identity_ledger_transaction"
  val IdentityStateTableName = s"${SchemaName}.identity_ledger_state"
}
