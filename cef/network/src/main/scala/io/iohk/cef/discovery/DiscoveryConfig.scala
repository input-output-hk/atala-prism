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
    val discoveryConfig = etcClientConfig.getConfig("discovery")
    val bootstrapNodes = NodeParser.parseNodes(discoveryConfig.getConfigList("bootstrapNodes").asScala.toSet)
    val blacklistDuration = {
      val duration = discoveryConfig.getDuration("blacklistDefaultDuration")
      FiniteDuration(duration.toNanos, TimeUnit.NANOSECONDS)
    }

    DiscoveryConfig(
      discoveryEnabled = discoveryConfig.getBoolean("enabled"),
      interface = discoveryConfig.getString("interface"),
      port = discoveryConfig.getInt("port"),
      bootstrapNodes = bootstrapNodes,
      discoveredNodesLimit = discoveryConfig.getInt("discoveredNodesLimit"),
      scanNodesLimit = discoveryConfig.getInt("scanNodesLimit"),
      concurrencyDegree = discoveryConfig.getInt("concurrencyDegree"),
      scanInitialDelay = discoveryConfig.getDuration("scanInitialDelay").toMillis.millis,
      scanInterval = discoveryConfig.getDuration("scanInterval").toMillis.millis,
      messageExpiration = discoveryConfig.getDuration("messageExpiration").toMillis.millis,
      maxSeekResults = discoveryConfig.getInt("maxSeekResults"),
      multipleConnectionsPerAddress = discoveryConfig.getBoolean("multipleConnectionsPerAddress"),
      blacklistDefaultDuration = blacklistDuration)
  }

}
