package io.iohk.cef.db

import java.time.Clock

import io.iohk.cef.network.Node
import org.bouncycastle.util.encoders.Hex
import scalikejdbc._
import scalikejdbc.config._


class ScalikeKnownNodeStorage(clock: Clock) extends KnownNodesStorage {

  Class.forName("org.h2.Driver")
  DBs.setupAll()

  implicit val session = AutoSession

  Schema.nodeTable.execute().apply()
  Schema.knownNodeTable.execute().apply()


  override def blacklist(node: Node): Unit = ???

  override def insert(node: Node): Long = {
    sql"""insert into node (id, discovery_address, discovery_port, server_address, server_port, capabilities)
         values(${Hex.toHexString(node.id.toArray)},
         ${Hex.toHexString(node.discoveryAddress.getAddress.getAddress)},
         ${node.discoveryAddress.getPort},
         ${Hex.toHexString(node.serverAddress.getAddress.getAddress)},
         ${node.serverAddress.getPort},
         ${Hex.toHexString(Array(node.capabilities.byte))})""".update().apply()
  }

  override def getAll(): Set[KnownNode] = {
    sql"""select node.id as node_id, discovery_address, discovery_port, server_address, server_port, capabilities
         from node inner join known_node on node.id = known_node.node_id
       """.map( rs => KnownNode(rs)).list.apply().toSet
  }

  override def remove(node: Node): Unit = {
    DB localTx { implicit session =>
      sql"""delete from known_node where node_id = ${Hex.toHexString(node.id.toArray)}""".executeUpdate.apply
      sql"""delete from node where id = ${Hex.toHexString(node.id.toArray)}""".executeUpdate.apply
    }
  }
}
