package io.iohk.cef.network.discovery.db

import java.net.{InetAddress, InetSocketAddress}
import java.time.Instant

import akka.util.ByteString
import io.iohk.cef.network.{Capabilities, NodeInfo}
import io.iohk.cef.utils.HexStringCodec._
import scalikejdbc.{WrappedResultSet, _}

case class KnownNodeTable(
    id: ByteString,
    discoveryAddress: Array[Byte],
    discoveryPort: Int,
    serverAddress: Array[Byte],
    serverPort: Int,
    capabilities: Capabilities,
    nodeId: ByteString,
    lastSeen: Instant,
    discovered: Instant)

object KnownNodeTable extends SQLSyntaxSupport[KnownNodeTable] {
  override val tableName = Schema.knownNodeTableName

  def apply(kn: ResultName[KnownNodeTable])(rs: WrappedResultSet): KnownNode = new KnownNode(
    NodeInfo(
      fromHexString(rs.string(kn.id)),
      new InetSocketAddress(InetAddress.getByAddress(rs.bytes(kn.discoveryAddress)), rs.int(kn.discoveryPort)),
      new InetSocketAddress(InetAddress.getByAddress(rs.bytes(kn.serverAddress)), rs.int(kn.serverPort)),
      Capabilities(rs.bytes(kn.capabilities)(0))
    ),
    rs.timestamp(kn.discovered).toInstant,
    rs.timestamp(kn.lastSeen).toInstant
  )
}

case class BlacklistNodeTable(nodeId: ByteString, blacklistSince: Instant, blacklistUntil: Instant)

object BlacklistNodeTable extends SQLSyntaxSupport[BlacklistNodeTable] {
  override val tableName =
    Schema.blacklistNodeTableName
}
