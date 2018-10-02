package io.iohk.cef.network
import com.typesafe.config.Config
import io.iohk.cef.network.discovery.DiscoveryConfig

class NetworkConfig(
    val discoveryConfig: DiscoveryConfig,
    val serverConfig: ServerConfig
)

object NetworkConfig {
  def apply(config: Config): NetworkConfig = {
    new NetworkConfig(DiscoveryConfig(config.getConfig("discovery")), ServerConfig(config.getConfig("server")))
  }
}
