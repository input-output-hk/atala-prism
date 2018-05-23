package io.iohk.cef.db
import scalikejdbc._

object Schema {

  val nodeTable =
    sql"""create table if not exists node (id char(256) primary key,
      discovery_address binary not null,
      discovery_port int not null,
      server_address binary not null,
      server_port int not null,
      capabilities binary not null
      );
      """

  val knownNodeTable =
    sql"""create table if not exists known_node (node_id char(256) not null,
      discovered timestamp with time zone not null,
      last_seen timestamp with time zone not null
      );
    """
}
