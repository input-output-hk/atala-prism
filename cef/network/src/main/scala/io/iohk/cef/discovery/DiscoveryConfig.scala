package io.iohk.cef.discovery

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import akka.util.ByteString
import io.iohk.cef.network.{Capabilities, Node, NodeParser}

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

  //TODO: properly represent bootstrap nodes
  def fromConfig(config: com.typesafe.config.Config) = {
    import scala.collection.JavaConverters._
    new DiscoveryConfig(
      discoveryEnabled = config.getBoolean("discovery.enabled"),
      interface = config.getString("discovery.interface"),
      port = config.getInt("discovery.port"),
      bootstrapNodes = config.getStringList("discovery.bootstrapNodes").asScala.map(line =>{
        val split = line.split(":")
        val address = new InetSocketAddress(split(1), split(2).toInt)
        Node(ByteString(split(0)), address, address, Capabilities(split(3).toByte))
      }).toSet,
      discoveredNodesLimit = config.getInt("discovery.discoveredNodesLimit"),
      scanNodesLimit = config.getInt("discovery.discoveredNodesLimit"),
      concurrencyDegree = config.getInt("discovery.concurrencyDegree"),
      scanInitialDelay = FiniteDuration(config.getLong("discovery.scanInitialDelay"), TimeUnit.MILLISECONDS),
      scanInterval = FiniteDuration(config.getLong("discovery.scanInterval"), TimeUnit.MILLISECONDS),
      messageExpiration = FiniteDuration(config.getLong("discovery.messageExpiration"), TimeUnit.MILLISECONDS),
      maxSeekResults = config.getInt("discovery.maxSeekResults"),
      multipleConnectionsPerAddress = config.getBoolean("discovery.multipleConnectionsPerAddress"),
      blacklistDefaultDuration = FiniteDuration(config.getLong("discovery.blacklistDefaultDuration"), TimeUnit.MILLISECONDS)
    )
  }

}
