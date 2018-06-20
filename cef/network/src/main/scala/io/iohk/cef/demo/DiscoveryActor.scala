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
import io.iohk.cef.network.{Node, ServerStatus}
import io.iohk.cef.telemetery.DatadogTelemetry
import org.bouncycastle.util.encoders.Hex

import scala.concurrent.duration._

object DiscoveryActor extends DatadogTelemetry {

  def discoveryBehavior(node: Node,
                        bootstrapNodeUris: Set[URI],
                        knownNodeStorage: KnownNodeStorage): Behavior[DiscoveryRequest] = {

    import io.iohk.cef.encoding.rlp.RLPEncoders._
    import io.iohk.cef.encoding.rlp.RLPImplicits._

    val state = NodeState(
      key = node.id,
      serverStatus = ServerStatus.Listening(node.serverAddress),
      discoveryStatus = ServerStatus.NotListening,
      capabilities = node.capabilities
    )

    val bootstrapNodes = bootstrapNodeUris.map(nodeUri =>
      Node(
        id = nodeId(nodeUri),
        serverAddress = new InetSocketAddress(nodeUri.getHost, nodeUri.getPort),
        discoveryAddress = new InetSocketAddress(nodeUri.getHost, discoveryPort(nodeUri.getPort)),
        capabilities = node.capabilities))

    val discoveryConfig = DiscoveryConfig(
      discoveryEnabled = true,
      interface = node.discoveryAddress.getHostName,
      port = node.discoveryAddress.getPort,
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
      registry)
  }


  // TODO this convention is duplicated
  private def discoveryPort(serverPort: Int): Int = serverPort + 1

  private def nodeId(nodeUri: URI): ByteString =
    ByteString(Hex.decode(nodeUri.getUserInfo))
}
