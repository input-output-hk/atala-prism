package io.iohk.cef.data.storage.scalike

object Schema {

  val SchemaName = "cef"

  val DataTableName = s"${SchemaName}.data_table"
  val DataItemSignatureTableName = s"${SchemaName}.data_item_signature"
  val DataItemOwnerTableName = s"${SchemaName}.data_item_owner"
}
