package io.iohk.cef.db

import java.time.Clock

import io.iohk.cef.network.Node
import org.bouncycastle.util.encoders.Hex
import scalikejdbc._
import scalikejdbc.config._

import scala.concurrent.duration.FiniteDuration


class KnownNodeStorageImpl(clock: Clock, dbName: Symbol = 'default) extends KnownNodeStorage {

  DBs.setup(dbName)

  override def blacklist(node: Node, duration: FiniteDuration): Unit = {
    val until = clock.instant().plusMillis(duration.toMillis)
    val nodeColumn = NodeTable.column
    val blacklistColumn = BlacklistNodeTable.column

    inTx { implicit session =>
      mergeNodeStatement(node, nodeColumn)

      sql"""merge into ${BlacklistNodeTable.table} (
        ${blacklistColumn.nodeId},
        ${blacklistColumn.blacklistSince},
        ${blacklistColumn.blacklistUntil}
      ) key(${blacklistColumn.nodeId})
       values(${Hex.toHexString(node.id.toArray)},
       ${clock.instant()},
       ${until})""".update().apply()
    }
  }

  override def insert(node: Node): Long = {
    val nodeColumn = NodeTable.column
    val knownNodeColumn = KnownNodeTable.column
    val kn = KnownNodeTable.syntax("kn")

    inTx { implicit session =>
      val discovered =
        sql"""
           select ${kn.discovered} from ${KnownNodeTable as kn} where ${kn.nodeId} = ${Hex.toHexString(node.id.toArray)}
         """.map(_.timestamp(knownNodeColumn.discovered).toInstant).single().apply()

      mergeNodeStatement(node, nodeColumn)

      sql"""merge into ${KnownNodeTable.table} (
        ${knownNodeColumn.nodeId},
        ${knownNodeColumn.lastSeen},
        ${knownNodeColumn.discovered}
      ) key(${knownNodeColumn.nodeId})
       values(${Hex.toHexString(node.id.toArray)},
       ${clock.instant()},
       ${discovered.getOrElse(clock.instant())})""".update().apply()
    }
  }

  private def mergeNodeStatement(node: Node, nodeColumn: scalikejdbc.ColumnName[NodeTable])(implicit session: DBSession) = {
    sql"""merge into ${NodeTable.table} (
          ${nodeColumn.id},
          ${nodeColumn.discoveryAddress},
          ${nodeColumn.discoveryPort},
          ${nodeColumn.serverAddress},
          ${nodeColumn.serverPort},
          ${nodeColumn.capabilities}
       ) key (${nodeColumn.id})
       values (${Hex.toHexString(node.id.toArray)},
         ${Hex.toHexString(node.discoveryAddress.getAddress.getAddress)},
         ${node.discoveryAddress.getPort},
         ${Hex.toHexString(node.serverAddress.getAddress.getAddress)},
         ${node.serverAddress.getPort},
         ${node.capabilities.byte}
       );
       """.update().apply()
  }

  override def getAll(): Set[KnownNode] = {
    val (kn, n, bn) = (KnownNodeTable.syntax("kn"), NodeTable.syntax("n"), BlacklistNodeTable.syntax("bn"))

    inTx { implicit session =>
      sql"""select ${kn.result.*}, ${n.result.*}
         from ${KnownNodeTable as kn}
            inner join ${NodeTable as n} on ${n.id} = ${kn.nodeId}
            left outer join ${BlacklistNodeTable as bn} on ${n.id} = ${bn.nodeId} and ${bn.blacklistUntil} > ${clock.instant()}
         where ${bn.nodeId} is null;
       """.map(rs => KnownNodeTable(n.resultName, kn.resultName)(rs)).list.apply().toSet
    }
  }

  override def remove(node: Node): Unit = {
    val nodeColumn = NodeTable.column
    val knownNodeColumn = KnownNodeTable.column
    val blacklistNodeColumn = BlacklistNodeTable.column

    inTx { implicit session =>
      sql"""delete from ${KnownNodeTable.table} where ${knownNodeColumn.nodeId} = ${Hex.toHexString(node.id.toArray)}""".executeUpdate.apply
      sql"""delete from ${BlacklistNodeTable.table} where ${blacklistNodeColumn.nodeId} = ${Hex.toHexString(node.id.toArray)}""".executeUpdate.apply
      sql"""delete from ${NodeTable.table} where ${nodeColumn.id} = ${Hex.toHexString(node.id.toArray)}""".executeUpdate.apply
    }
  }

  def inTx[T](block: DBSession => T) = {
    DB localTx {
      block
    }
  }
}
