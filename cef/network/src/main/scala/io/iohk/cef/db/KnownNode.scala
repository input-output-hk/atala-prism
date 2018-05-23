package io.iohk.cef.db

import java.time.Instant

import io.iohk.cef.network.Node
import scalikejdbc._

case class KnownNode(node: Node, discovered: Instant, lastSeen: Instant)

object KnownNode extends SQLSyntaxSupport[KnownNode] {
  def apply(rs: WrappedResultSet) = new KnownNode(
    Node(rs),
    rs.timestamp("discovered").toInstant,
    rs.timestamp("last_seen").toInstant
  )
}

