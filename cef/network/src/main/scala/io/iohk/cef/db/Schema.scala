package io.iohk.cef.db

object Schema {

  val schemaName = "cef"

  val nodeTableName = s"${schemaName}.node"
  val knownNodeTableName = s"${schemaName}.known_node"
  val blacklistNodeTableName = s"${schemaName}.blacklist_node"

}
