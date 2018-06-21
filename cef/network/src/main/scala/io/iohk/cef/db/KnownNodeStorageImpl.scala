package io.iohk.cef.db

import java.time.{Clock, Instant}
import java.util.concurrent.atomic.AtomicInteger

import io.iohk.cef.network.Node
import io.iohk.cef.telemetery.DatadogTelemetry
import org.bouncycastle.util.encoders.Hex
import scalikejdbc._
import scalikejdbc.config._

import scala.concurrent.duration.FiniteDuration

/**
  * Implementation of the KnownNodeStorage using ScalikeJdbc.
  * General guidelines:
  * - Txs should be declared using the inTx method.
  * - The inTx method would provide a DBSession that can be declared implicit for executing the statements
  * - inTx should be called only in the public methods. Private methods should receive the DBSession as an implicit parameter.
  * @param clock
  * @param dbName
  */
class KnownNodeStorageImpl(clock: Clock, dbName: Symbol = 'default) extends KnownNodeStorage with DatadogTelemetry {

  DBs.setup(dbName)

  val trackingKnownNodes =
    registry.gauge("known_nodes", new AtomicInteger(getAll().size))

  override def blacklist(node: Node, duration: FiniteDuration): Unit = {
    val until = clock.instant().plusMillis(duration.toMillis)
    val blacklistColumn = BlacklistNodeTable.column

    inTx { implicit session =>
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
    val knownNodeColumn = KnownNodeTable.column
    val kn = KnownNodeTable.syntax("kn")

    inTx { implicit session =>
      val discovered = getDiscoveredInstant(node, knownNodeColumn, kn)
      mergeNodeStatement(node, discovered, knownNodeColumn)
    }
  }

  private def getDiscoveredInstant(node: Node,
                                   knownNodeColumn: ColumnName[KnownNodeTable],
                                   kn: QuerySQLSyntaxProvider[SQLSyntaxSupport[KnownNodeTable], KnownNodeTable])(
                                  implicit session: DBSession
  ) = {
    sql"""
           select ${kn.discovered} from ${KnownNodeTable as kn} where ${kn.id} = ${Hex.toHexString(node.id.toArray)}
         """.map(_.timestamp(knownNodeColumn.discovered).toInstant).single().apply()
  }

  override def getAll(): Set[KnownNode] = {
    val (kn, bn) = (KnownNodeTable.syntax("kn"),BlacklistNodeTable.syntax("bn"))

    inTx { implicit session =>
      sql"""select ${kn.result.*}, ${kn.result.*}
         from ${KnownNodeTable as kn}
            left outer join ${BlacklistNodeTable as bn} on ${kn.id} = ${bn.nodeId} and ${bn.blacklistUntil} > ${clock.instant()}
         where ${bn.nodeId} is null;
       """.map(rs => KnownNodeTable(kn.resultName)(rs)).list.apply().toSet
    }
  }

  override def remove(node: Node): Unit = {
    val knownNodeColumn = KnownNodeTable.column
    val blacklistNodeColumn = BlacklistNodeTable.column

    val result = inTx { implicit session =>
      sql"""delete from ${BlacklistNodeTable.table} where ${blacklistNodeColumn.nodeId} = ${Hex.toHexString(node.id.toArray)}""".executeUpdate.apply
      sql"""delete from ${KnownNodeTable.table} where ${knownNodeColumn.id} = ${Hex.toHexString(node.id.toArray)}""".executeUpdate.apply
    }
    if (result > 0) trackingKnownNodes.decrementAndGet()
  }

//  def getBlacklisted(): Set[BlacklistNode] = {
//    val (n, bn) = (NodeTable.syntax("n"), BlacklistNodeTable.syntax("bn"))
//
//    inTx { implicit session =>
//      sql"""select ${n.result.*}, ${bn.result.*}
//         from ${NodeTable as n}
//            inner join ${BlacklistNodeTable as bn} on ${n.id} = ${bn.nodeId} and ${bn.blacklistUntil} <= ${clock.instant()};
//       """.map(rs => BlacklistNodeTable(n.resultName, bn.resultName)(rs)).list.apply().toSet
//    }
//  }

  /**
    * Wraps the block into a db transaction. Method created for the purpose of testing
    * @param block
    * @tparam T
    * @return
    */
  protected def inTx[T](block: DBSession => T) = {
    DB localTx {
      block
    }
  }

  private def mergeNodeStatement(node: Node, discovered: Option[Instant], nodeColumn: scalikejdbc.ColumnName[KnownNodeTable])(implicit session: DBSession) = {
    sql"""merge into ${KnownNodeTable.table} (
          ${nodeColumn.id},
          ${nodeColumn.discoveryAddress},
          ${nodeColumn.discoveryPort},
          ${nodeColumn.serverAddress},
          ${nodeColumn.serverPort},
          ${nodeColumn.capabilities},
          ${nodeColumn.lastSeen},
          ${nodeColumn.discovered}

       ) key (${nodeColumn.id})
       values (${Hex.toHexString(node.id.toArray)},
         ${Hex.toHexString(node.discoveryAddress.getAddress.getAddress)},
         ${node.discoveryAddress.getPort},
         ${Hex.toHexString(node.serverAddress.getAddress.getAddress)},
         ${node.serverAddress.getPort},
         ${node.capabilities.byte},
         ${clock.instant()},
         ${discovered.getOrElse(clock.instant())}
       );
       """.update().apply()
  }
}
