package io.iohk.cef.discovery

import java.util.concurrent.TimeUnit

import io.iohk.cef.network.{Node, NodeParser}

import scala.concurrent.duration.{FiniteDuration, _}

case class DiscoveryConfig(
                            discoveryEnabled: Boolean,
                            interface: String,
                            port: Int,
                            bootstrapNodes: Set[Node],
                            discoveredNodesLimit: Int,
                            scanNodesLimit: Int,
                            concurrencyDegree: Int,
                            scanInitialDelay: FiniteDuration,
                            scanInterval: FiniteDuration,
                            messageExpiration: FiniteDuration,
                            maxSeekResults: Int,
                            multipleConnectionsPerAddress: Boolean,
                            blacklistDefaultDuration: FiniteDuration)

object DiscoveryConfig {
  def apply(etcClientConfig: com.typesafe.config.Config): DiscoveryConfig = {
    import scala.collection.JavaConverters._
    val discoveryConfig = etcClientConfig.getConfig("network.discovery")
    val bootstrapNodes = NodeParser.parseNodes(discoveryConfig.getStringList("bootstrap-nodes").asScala.toSet)
    val blacklistDuration = {
      val duration = discoveryConfig.getDuration("blacklist-duration")
      FiniteDuration(duration.toNanos, TimeUnit.NANOSECONDS)
    }

    DiscoveryConfig(
      discoveryEnabled = discoveryConfig.getBoolean("discovery-enabled"),
      interface = discoveryConfig.getString("interface"),
      port = discoveryConfig.getInt("port"),
      bootstrapNodes = bootstrapNodes,
      discoveredNodesLimit = discoveryConfig.getInt("nodes-limit"),
      scanNodesLimit = discoveryConfig.getInt("scan-nodes-limit"),
      concurrencyDegree = discoveryConfig.getInt("concurrency-degree"),
      scanInitialDelay = discoveryConfig.getDuration("scan-initial-delay").toMillis.millis,
      scanInterval = discoveryConfig.getDuration("scan-interval").toMillis.millis,
      messageExpiration = discoveryConfig.getDuration("message-expiration").toMillis.millis,
      maxSeekResults = discoveryConfig.getInt("max-seek-results"),
      multipleConnectionsPerAddress = discoveryConfig.getBoolean("multiple-connection-per-address"),
      blacklistDefaultDuration = blacklistDuration)
  }

}
