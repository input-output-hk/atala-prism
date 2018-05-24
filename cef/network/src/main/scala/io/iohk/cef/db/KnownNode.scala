package io.iohk.cef.db

import java.time.Instant

import io.iohk.cef.db.Schema.KnownNodeTableColumn
import io.iohk.cef.network.Node
import scalikejdbc._

case class KnownNode(node: Node, discovered: Instant, lastSeen: Instant)

object KnownNode extends SQLSyntaxSupport[KnownNode] {

  import anorm._

  def knownNodeParser: RowParser[KnownNode] = {
    Node.nodeParser ~
      RowParsers.instant(KnownNodeTableColumn.discovered) ~
      RowParsers.instant(KnownNodeTableColumn.lastSeen) map {
      case node ~ discovered ~ lastSeen =>
        KnownNode(node, discovered, lastSeen)
    }
  }

  def apply(rs: WrappedResultSet) = new KnownNode(
    Node(rs),
    rs.timestamp("discovered").toInstant,
    rs.timestamp("last_seen").toInstant
  )
}

