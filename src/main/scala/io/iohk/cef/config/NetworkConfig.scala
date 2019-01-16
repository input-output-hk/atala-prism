package io.iohk.cef.config

import java.time.Clock

import io.iohk.cef.network.discovery.DiscoveryConfig
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.network.{NetworkServices, PeerConfig}

case class NetworkConfig(peerConfig: PeerConfig, discoveryConfig: DiscoveryConfig) {

  private val clock = Clock.systemUTC()

  lazy val transports = new Transports(peerConfig)
  lazy val discovery = NetworkServices.networkDiscovery(clock, peerConfig, discoveryConfig)
}
