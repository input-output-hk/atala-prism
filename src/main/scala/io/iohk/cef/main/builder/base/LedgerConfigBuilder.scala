package io.iohk.cef.main.builder.base
import java.time.Clock

import io.iohk.cef.LedgerId
import io.iohk.cef.main.builder.helpers.LedgerConfig
import io.iohk.cef.network.NodeId
import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.transport.Transports

trait LedgerConfigBuilder {
  val clock: Clock
  val nodeId: NodeId
  val ledgerId: LedgerId

  val ledgerConfig: LedgerConfig
  //Network
  val transports: Transports
  val networkDiscovery: NetworkDiscovery
}
