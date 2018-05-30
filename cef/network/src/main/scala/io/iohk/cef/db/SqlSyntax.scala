package io.iohk.cef.db

import java.net.{InetAddress, InetSocketAddress}
import java.time.Instant

import akka.util.ByteString
import io.iohk.cef.db.Schema.{KnownNodeTableColumn, NodeTableColumn}
import io.iohk.cef.network.{Capabilities, Node}
import org.bouncycastle.util.encoders.Hex
import scalikejdbc.WrappedResultSet
import scalikejdbc._

case class KnownNodeTable(nodeId: ByteString, lastSeen: Instant, discovered: Instant)

object KnownNodeTable extends SQLSyntaxSupport[KnownNodeTable] {
  override val tableName = KnownNodeTableColumn.tableName

  def apply(n: ResultName[NodeTable], kn: ResultName[KnownNodeTable])(rs: WrappedResultSet) = new KnownNode(
    NodeTable(n)(rs),
    rs.timestamp(kn.discovered).toInstant,
    rs.timestamp(kn.lastSeen).toInstant
  )
}

case class NodeTable(id: ByteString,
                    discoveryAddress: Array[Byte],
                    discoveryPort: Int,
                    serverAddress: Array[Byte],
                    serverPort: Int,
                    capabilities: Capabilities)

object NodeTable extends SQLSyntaxSupport[NodeTable] {
  override val tableName = NodeTableColumn.tableName

  def apply(n: ResultName[NodeTable])(rs: WrappedResultSet) = Node(
    ByteString(Hex.decode(rs.string(n.id))),
    new InetSocketAddress(InetAddress.getByAddress(rs.bytes(n.discoveryAddress)), rs.int(n.discoveryPort)),
    new InetSocketAddress(InetAddress.getByAddress(rs.bytes(n.serverAddress)), rs.int(n.serverPort)),
    Capabilities(rs.bytes(n.capabilities)(0))
  )
}
