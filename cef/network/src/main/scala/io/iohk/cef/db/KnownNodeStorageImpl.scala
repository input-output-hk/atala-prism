package io.iohk.cef.db

import java.time.Clock

import io.iohk.cef.db.Schema.KnownNodeTableColumn
import io.iohk.cef.network.Node
import org.bouncycastle.util.encoders.Hex
import scalikejdbc._
import scalikejdbc.config._


class KnownNodeStorageImpl(clock: Clock) extends KnownNodesStorage {

  Class.forName("org.h2.Driver")
  DBs.setupAll()

  implicit val session = AutoSession

  override def blacklist(node: Node): Unit = ???

  override def insert(node: Node): Long = {
    DB localTx { implicit session =>
      val nodeColumn = NodeTable.column
      val knownNodeColumn = KnownNodeTable.column

      sql"""merge into ${NodeTable.table} (
          ${nodeColumn.id},
          ${nodeColumn.discoveryAddress},
          ${nodeColumn.discoveryPort},
          ${nodeColumn.serverAddress},
          ${nodeColumn.serverPort},
          ${nodeColumn.capabilities}
       ) key (${nodeColumn.id})
       values (${Hex.toHexString(node.id.toArray)},
         ${node.discoveryAddress.getAddress.getAddress},
         ${node.discoveryAddress.getPort},
         ${node.serverAddress.getAddress.getAddress},
         ${node.serverAddress.getPort},
         ${Array(node.capabilities.byte)}
       );
       """.update().apply()

      sql"""merge into ${KnownNodeTable.table} (
          ${knownNodeColumn.nodeId},
          ${knownNodeColumn.lastSeen},
          ${knownNodeColumn.discovered}
      ) key(${knownNodeColumn.nodeId})
       values(${Hex.toHexString(node.id.toArray)},
       ${clock.instant()},
       ${clock.instant()})""".update().apply()
    }
  }

  override def getAll(): Set[KnownNode] = {
    val (kn, n) = (KnownNodeTable.syntax("kn"), NodeTable.syntax("n"))

    sql"""select ${kn.result.*}, ${n.result.*}
         from ${KnownNodeTable as kn} inner join ${NodeTable as n} on ${n.id} = ${kn.nodeId}
       """.map( rs => KnownNodeTable(n.resultName, kn.resultName)(rs)).list.apply().toSet
  }

  override def remove(node: Node): Unit = {
    val nodeColumn = NodeTable.column
    val knownNodeColumn = KnownNodeTable.column

    val nodeid = sqls"${KnownNodeTableColumn.nodeId}"

    DB localTx { implicit session =>
      sql"""delete from ${KnownNodeTable.table} where ${knownNodeColumn.nodeid} = ${Hex.toHexString(node.id.toArray)}""".executeUpdate.apply
      sql"""delete from ${NodeTable.table} where ${nodeColumn.id} = ${Hex.toHexString(node.id.toArray)}""".executeUpdate.apply
    }
  }
}
