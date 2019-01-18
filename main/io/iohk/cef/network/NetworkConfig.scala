package io.iohk.cef.network

import java.time.Clock

import io.iohk.cef.network.discovery.DiscoveryConfig
import io.iohk.cef.network.transport.Transports

case class NetworkConfig(peerConfig: PeerConfig, discoveryConfig: DiscoveryConfig) {

  private val clock = Clock.systemUTC()

  lazy val transports = new Transports(peerConfig)
  lazy val discovery = NetworkServices.networkDiscovery(clock, peerConfig, discoveryConfig)
}
