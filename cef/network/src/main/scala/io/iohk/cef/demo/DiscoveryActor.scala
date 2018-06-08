package io.iohk.cef.demo

import java.net.{InetSocketAddress, URI}
import java.security.SecureRandom
import java.time.Clock

import akka.actor.typed.Behavior
import akka.util.ByteString
import io.iohk.cef.db.KnownNodeStorage
import io.iohk.cef.discovery.DiscoveryManager.DiscoveryRequest
import io.iohk.cef.discovery._
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.NodeStatus.NodeState
import io.iohk.cef.network.{Capabilities, Node, ServerStatus}
import io.iohk.cef.telemetery.DatadogRegistryConfig
import org.bouncycastle.util.encoders.Hex

import scala.concurrent.duration._

object DiscoveryActor {

  def discoveryBehavior(uri: URI,
                        bootstrapNodeUris: Set[URI],
                        capabilities: Capabilities,
                        knownNodeStorage: KnownNodeStorage): Behavior[DiscoveryRequest] = {
    import io.iohk.cef.encoding.rlp.RLPEncoders._
    import io.iohk.cef.encoding.rlp.RLPImplicits._

    val state = NodeState(
      key = toNodeKey(uri),
      serverStatus = ServerStatus.Listening(localhost(uri.getPort)),
      discoveryStatus = ServerStatus.NotListening,
      capabilities = capabilities)

    val discoveryConfig = config(uri, bootstrapNodeUris, capabilities)

    val encoder = implicitly[Encoder[DiscoveryWireMessage, ByteString]]

    val decoder = implicitly[Decoder[ByteString, DiscoveryWireMessage]]

    DiscoveryManager.behaviour(
      discoveryConfig,
      knownNodeStorage,
      state,
      Clock.systemUTC(),
      encoder,
      decoder,
      context => context.spawn(
        DiscoveryListener.behavior(discoveryConfig,
          UDPBridge.creator(discoveryConfig, encoder, decoder)),
        "DiscoveryListener"),
      new SecureRandom(),
      DatadogRegistryConfig.registry)
  }

  private def localhost(port: Int): InetSocketAddress =
    new InetSocketAddress("localhost", port)

  private def config(uri: URI, bootstrapNodeUris: Set[URI], capabilities: Capabilities): DiscoveryConfig = {

    val bootstrapNodes = bootstrapNodeUris.map(nodeUri =>
      Node(
        id = toNodeKey(nodeUri),
        serverAddress = localhost(nodeUri.getPort),
        discoveryAddress = localhost(discoveryPort(nodeUri.getPort)),
        capabilities = capabilities))

    DiscoveryConfig(
      discoveryEnabled = true,
      interface = "0.0.0.0",
      port = discoveryPort(uri.getPort),
      bootstrapNodes = bootstrapNodes,
      discoveredNodesLimit = 10,
      scanNodesLimit = 10,
      concurrencyDegree = 20,
      scanInitialDelay = 10.seconds,
      scanInterval = 10.seconds,
      messageExpiration = 100.seconds,
      maxSeekResults = 10,
      multipleConnectionsPerAddress = true,
      blacklistDefaultDuration = 30 seconds
    )
  }

  private def discoveryPort(serverPort: Int): Int = serverPort + 1

  private def toNodeKey(nodeUri: URI): ByteString =
    ByteString(Hex.decode(nodeUri.getUserInfo))
}
