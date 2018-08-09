package io.iohk.cef.network.discovery.db

object Schema {

  val schemaName = "cef"

  val knownNodeTableName = s"${schemaName}.known_node"
  val blacklistNodeTableName = s"${schemaName}.blacklist_node"

}
