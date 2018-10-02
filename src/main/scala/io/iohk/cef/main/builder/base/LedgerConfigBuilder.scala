package io.iohk.cef.main.builder.base
import java.time.Clock

import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.main.builder.helpers.LedgerConfig
import io.iohk.cef.network.discovery.{DiscoveryWireMessage, NetworkDiscovery}
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.network.{NetworkConfig, NodeId}

trait LedgerConfigBuilder {
  val clock: Clock
  val nodeId: NodeId

  val ledgerConfig: LedgerConfig
  //Network
  val networkConfig: NetworkConfig
  val nodeIdStr: String
  val transports: Transports
  def networkDiscovery(implicit discoveryMsgSerializer: ByteStringSerializable[DiscoveryWireMessage]): NetworkDiscovery
}
