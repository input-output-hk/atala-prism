package io.iohk.cef.discovery

import io.iohk.cef.network.{NodeAddress, NodeParser}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

case class DiscoveryConfig(
                            discoveryEnabled: Boolean,
                            interface: String,
                            port: Int,
                            bootstrapNodes: Set[NodeAddress],
                            nodesLimit: Int,
                            scanMaxNodes: Int,
                            scanInitialDelay: FiniteDuration,
                            scanInterval: FiniteDuration,
                            messageExpiration: FiniteDuration,
                            maxSeekResults: Int)

object DiscoveryConfig {
  def apply(etcClientConfig: com.typesafe.config.Config): DiscoveryConfig = {
    import scala.collection.JavaConverters._
    val discoveryConfig = etcClientConfig.getConfig("network.discovery")
    val bootstrapNodes = NodeParser.parseNodes(discoveryConfig.getStringList("bootstrap-nodes").asScala.toSet)

    DiscoveryConfig(
      discoveryEnabled = discoveryConfig.getBoolean("discovery-enabled"),
      interface = discoveryConfig.getString("interface"),
      port = discoveryConfig.getInt("port"),
      bootstrapNodes = bootstrapNodes,
      nodesLimit = discoveryConfig.getInt("nodes-limit"),
      scanMaxNodes = discoveryConfig.getInt("scan-max-nodes"),
      scanInitialDelay = discoveryConfig.getDuration("scan-initial-delay").toMillis.millis,
      scanInterval = discoveryConfig.getDuration("scan-interval").toMillis.millis,
      messageExpiration = discoveryConfig.getDuration("message-expiration").toMillis.millis,
      maxSeekResults = discoveryConfig.getInt("max-seek-results"))
  }

}
