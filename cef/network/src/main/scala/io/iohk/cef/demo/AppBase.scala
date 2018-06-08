package io.iohk.cef.demo

import java.net.{InetAddress, InetSocketAddress}
import java.security.SecureRandom
import java.time.Clock
import java.util.concurrent.TimeUnit

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.util.ByteString
import akka.{actor => untyped}
import com.typesafe.config.ConfigFactory
import io.iohk.cef.db.KnownNodeStorageImpl
import io.iohk.cef.discovery.DiscoveryManager.DiscoveryRequest
import io.iohk.cef.discovery._
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.NodeStatus.NodeState
import io.iohk.cef.network.{Capabilities, Node, ServerStatus}
import io.iohk.cef.telemetery.DatadogRegistryConfig
import io.iohk.cef.utils.Logger

import collection.JavaConverters._
import scala.concurrent.duration._

trait AppBase extends Logger {

  val configFile = ConfigFactory.load()

  val address: Array[Byte] = Array(127.toByte, 0, 0, 1)
  val localhost = InetAddress.getByAddress("", address)

  val discoveryConfig = new DiscoveryConfig(
    discoveryEnabled = configFile.getBoolean("discovery.enabled"),
    interface = configFile.getString("discovery.interface"),
    port = configFile.getInt("discovery.port"),
    bootstrapNodes = configFile.getStringList("discovery.bootstrapNodes").asScala.map(line =>{
      val split = line.split(":")
      val address = new InetSocketAddress(split(1), split(2).toInt)
      Node(ByteString(split(0)), address, address, Capabilities(split(3).toByte))
    }).toSet,
    discoveredNodesLimit = configFile.getInt("discovery.discoveredNodesLimit"),
    scanNodesLimit = configFile.getInt("discovery.discoveredNodesLimit"),
    concurrencyDegree = configFile.getInt("discovery.concurrencyDegree"),
    scanInitialDelay = FiniteDuration(configFile.getLong("discovery.scanInitialDelay"), TimeUnit.MILLISECONDS),
    scanInterval = FiniteDuration(configFile.getLong("discovery.scanInterval"), TimeUnit.MILLISECONDS),
    messageExpiration = FiniteDuration(configFile.getLong("discovery.messageExpiration"), TimeUnit.MILLISECONDS),
    maxSeekResults = configFile.getInt("discovery.maxSeekResults"),
    multipleConnectionsPerAddress = configFile.getBoolean("discovery.multipleConnectionsPerAddress"),
    blacklistDefaultDuration = FiniteDuration(configFile.getLong("discovery.blacklistDefaultDuration"), TimeUnit.MILLISECONDS)
  )

  import io.iohk.cef.encoding.rlp.RLPEncoders._
  import io.iohk.cef.encoding.rlp.RLPImplicits._

  val encoder = implicitly[Encoder[DiscoveryWireMessage, ByteString]]

  val decoder = implicitly[Decoder[ByteString, DiscoveryWireMessage]]

  def createActor(id: Int, bootstrapNodeIds: Set[Int], capabilities: Capabilities)
                 (implicit system: untyped.ActorSystem):
  ActorRef[DiscoveryRequest] = {

    val state = new NodeState(ByteString(id), ServerStatus.NotListening, ServerStatus.NotListening, capabilities)
    val portBase = configFile.getInt("demo.port")
    val address = configFile.getString("demo.address")
    val bootstrapNodes = bootstrapNodeIds.map(nodeId =>
      Node(ByteString(nodeId.toString),
        new InetSocketAddress(address, portBase + nodeId),
        new InetSocketAddress(address, portBase + nodeId), state.capabilities)
    )

    val config = discoveryConfig.copy(port = portBase + id, bootstrapNodes = bootstrapNodes)
    val secureRandom = new SecureRandom()

    val behavior = DiscoveryManager.behaviour(
      config,
      new KnownNodeStorageImpl(Clock.systemUTC()),
      state,
      Clock.systemUTC(),
      encoder,
      decoder,
      context => context.spawn(
        DiscoveryListener.behavior(config,
          UDPBridge.creator(config, encoder, decoder)), "DiscoveryListener"),
      secureRandom,
      DatadogRegistryConfig.registry
    )

    system.spawn(behavior, "discovery_behaviour")
  }
}
