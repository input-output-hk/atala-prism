package io.iohk.cef.db

object Schema {

  object NodeTableColumn {
    val id = "id"
    val discoveryAddress = "discovery_address"
    val discoveryPort = "discovery_port"
    val serverAddress = "server_address"
    val serverPort = "server_port"
    val capabilities = "capabilities"

    val tableName = s"${schemaName}.node"
  }

  val schemaName = "cef"

  val schema = s"create schema if not exists ${schemaName};"

  val nodeTable =
    s"""create table if not exists ${NodeTableColumn.tableName} (
      ${NodeTableColumn.id} char(256) primary key,
      ${NodeTableColumn.discoveryAddress} binary not null,
      ${NodeTableColumn.discoveryPort} int not null,
      ${NodeTableColumn.serverAddress} binary not null,
      ${NodeTableColumn.serverPort} int not null,
      ${NodeTableColumn.capabilities} binary not null
      );
      """

  object KnownNodeTableColumn {
    val nodeId = "node_id"
    val discovered = "discovered"
    val lastSeen = "last_seen"

    val tableName = s"${schemaName}.known_node"
  }

  val knownNodeTable =
    s"""create table if not exists ${KnownNodeTableColumn.tableName} (
      ${KnownNodeTableColumn.nodeId} char(256) not null,
      ${KnownNodeTableColumn.discovered} timestamp not null,
      ${KnownNodeTableColumn.lastSeen} timestamp not null
      );
    """

}
