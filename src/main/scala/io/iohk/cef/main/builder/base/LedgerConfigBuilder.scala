package io.iohk.cef.main.builder.base
import java.time.Clock

import io.iohk.cef.main.builder.helpers.LedgerConfig
import io.iohk.cef.network.discovery.NetworkDiscovery
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
  val networkDiscovery: NetworkDiscovery
}
