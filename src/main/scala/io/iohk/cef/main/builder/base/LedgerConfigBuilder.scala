package io.iohk.cef.main.builder.base
import java.net.InetSocketAddress
import java.time.Clock

import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.main.builder.helpers.LedgerConfig
import io.iohk.cef.network.discovery.{DiscoveryConfig, DiscoveryWireMessage, NetworkDiscovery}
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.network.{NodeId, PeerInfo}

trait LedgerConfigBuilder {
  val clock: Clock
  val nodeId: NodeId
  val ledgerId: LedgerId

  val ledgerConfig: LedgerConfig
  //Network
  val serverAddress: InetSocketAddress
  val discoveryAddress: InetSocketAddress
  val nodeIdStr: String
  val transports: Transports
  def networkDiscovery(implicit discoveryMsgSerializer: ByteStringSerializable[DiscoveryWireMessage]): NetworkDiscovery
  val discoveryConfig: DiscoveryConfig
  val bootstrapPeers: Set[PeerInfo]
}
