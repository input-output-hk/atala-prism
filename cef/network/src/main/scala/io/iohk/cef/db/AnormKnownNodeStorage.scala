package io.iohk.cef.db
import java.sql.Connection
import java.time.Clock

import akka.util.ByteString
import anorm._
import com.jolbox.bonecp.{BoneCP, BoneCPConfig}
import io.iohk.cef.db.Schema.{KnownNodeTableColumn, NodeTableColumn}
import io.iohk.cef.network.Node
import org.bouncycastle.util.encoders.Hex
import org.slf4j.LoggerFactory

class AnormKnownNodeStorage(clock: Clock, pool: ConnectionPool) extends KnownNodesStorage {

  setup

  val logger = LoggerFactory.getLogger(this.getClass)

  def connection: Connection = pool.getConnection.getOrElse {
    logger.error("Could not get the DB connection")
    throw new Exception("Could not get the DB connection")
  }

  def setup = withConnection { implicit c =>
    val schemaResult = SQL(Schema.schema).execute()
    val nodeTableResult = SQL(Schema.nodeTable).execute()
    val knownNodeTableResult = SQL(Schema.knownNodeTable).execute()
    //schemaResult.
  }

  override def insert(node: Node): Long =  withConnection { implicit c =>
    getNode(node.id) map { knownNode =>
      updateKnownNode(knownNode.copy(node = node, lastSeen = clock.instant()))
    } orElse {
      insertKnownNode(KnownNode(node, clock.instant(), clock.instant()))
    }
    0
  }

  private def updateKnownNode(knownNode: KnownNode)(implicit c: Connection) = {
    val node = knownNode.node
    SQL(
      s"""update ${NodeTableColumn.tableName} set
          ,
          ${NodeTableColumn.discoveryAddress} = {discovery_address},
          ${NodeTableColumn.discoveryPort} = {discovery_port},
          ${NodeTableColumn.serverAddress} = {server_address},
          ${NodeTableColumn.serverPort} = {server_port},
          ${NodeTableColumn.capabilities} = {capabilities}
         where ${NodeTableColumn.id} = {id},
        """).on("id" -> Hex.toHexString(node.id.toArray)
    ).executeUpdate()
    SQL(
      s"""update ${KnownNodeTableColumn.tableName} set
          ${KnownNodeTableColumn.discovered} = {discovered},
          ${KnownNodeTableColumn.lastSeen} = {last_seen}
         where ${KnownNodeTableColumn.nodeId} = {id}
       """
    ).on(
      "id" -> Hex.toHexString(node.id.toArray)
    ).executeUpdate()
  }

  private def insertKnownNode(knownNode: KnownNode)(implicit c: Connection) = {
    val node = knownNode.node
    SQL(
      s"""insert into ${NodeTableColumn.tableName} (
          ${NodeTableColumn.id},
          ${NodeTableColumn.discoveryAddress},
          ${NodeTableColumn.discoveryPort},
          ${NodeTableColumn.serverAddress},
          ${NodeTableColumn.serverPort},
          ${NodeTableColumn.capabilities}
         )
         values({id},
         {discovery_address},
         {discovery_port},
         {server_address},
         {server_port},
         {capabilities})""").on(
      "id" -> Hex.toHexString(node.id.toArray),
      "discovery_address" -> Hex.toHexString(node.discoveryAddress.getAddress.getAddress),
      "discovery_port" -> node.discoveryAddress.getPort,
      "server_address" -> Hex.toHexString(node.serverAddress.getAddress.getAddress),
      "server_port" -> node.serverAddress.getPort,
      "capabilities" -> Hex.toHexString(Array(node.capabilities.byte))
    ).executeInsert()
    SQL(
      s"""insert into ${KnownNodeTableColumn.tableName} (
          ${KnownNodeTableColumn.nodeId},
          ${KnownNodeTableColumn.discovered},
          ${KnownNodeTableColumn.lastSeen}
         )
         values ({id}, {discovered}, {last_seen})
       """
    ).on(
      "id" -> Hex.toHexString(node.id.toArray),
      "discovered" -> knownNode.discovered,
      "last_seen" -> knownNode.lastSeen
    ).executeInsert()
  }

  override def blacklist(node: Node): Unit = ???

  override def remove(node: Node): Unit =  withConnection { implicit c =>
    SQL(s"delete from ${NodeTableColumn.tableName} where ${NodeTableColumn.id} = {id}")
      .on("id" -> Hex.toHexString(node.id.toArray)).executeUpdate()
    SQL(s"delete from ${KnownNodeTableColumn.tableName} where ${KnownNodeTableColumn.nodeId} = {id}")
      .on("id" -> Hex.toHexString(node.id.toArray)).executeUpdate()
  }

  override def getAll(): Set[KnownNode] =  withConnection { implicit c =>
    SQL(s"""select *
           from ${NodeTableColumn.tableName} inner join
              ${KnownNodeTableColumn.tableName} on node.id = known_node.node_id""")
      .as(KnownNode.knownNodeParser.*).toSet
  }

  def getNode(id: ByteString): Option[KnownNode] = withConnection { implicit c =>
    logger.debug(s"Fetching node with id: ${Hex.toHexString(id.toArray)}")
    val node = SQL(
      s"""select *
        from ${NodeTableColumn.tableName} inner join
          ${KnownNodeTableColumn.tableName} on node.id = known_node.node_id
        where id = {id}""")
      .on("id" -> Hex.toHexString(id.toArray))
      .as(KnownNode.knownNodeParser.singleOpt)
    logger.debug(s"Node found was ${node}")
    node
  }


  def withConnection[T](exec: Connection => T) = {
    implicit val conn = connection
    conn.setAutoCommit(true)
    val result = exec(conn)
    conn.commit()
    conn.close()
    result
  }
}
class ConnectionPool(databaseName: String) {
  val logger = LoggerFactory.getLogger(this.getClass)

  private val connectionPool = {

    try {
      Class.forName("org.h2.Driver")
      val config = new BoneCPConfig()
      config.setJdbcUrl(s"jdbc:h2:file:./db/${databaseName}")
      config.setUsername("sa")
      config.setPassword("")
      config.setMinConnectionsPerPartition(2)
      config.setMaxConnectionsPerPartition(5)
      config.setPartitionCount(2)
      config.setCloseConnectionWatch(true) // if connection is not closed throw exception
      config.setLogStatementsEnabled(true) // for debugging purpose
      Some(new BoneCP(config))
    } catch {
      case exception: Exception => ;
        logger.error("Error in creation of connection pool" + exception.printStackTrace())
        None
    }
  }

  def getConnection: Option[Connection] = {
    connectionPool match {
      case Some(connPool) => Some(connPool.getConnection)
      case None => None
    }
  }
}
