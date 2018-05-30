package io.iohk.cef.db

object Schema {

  object KnownNodeTableColumn {
    val nodeId = "node_id"
    val discovered = "discovered"
    val lastSeen = "last_seen"

    val tableName = s"${schemaName}.known_node"
  }

  val schemaName = "cef"

  object NodeTableColumn {
    val id = "id"
    val discoveryAddress = "discovery_address"
    val discoveryPort = "discovery_port"
    val serverAddress = "server_address"
    val serverPort = "server_port"
    val capabilities = "capabilities"

    val tableName = s"${schemaName}.node"
  }
}
