package io.iohk.cef.demo

import java.net.URI
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

object DiscoveryActor extends DatadogTelemetry {

  def discoveryBehavior(node: Node,
                        discoveryConfig: DiscoveryConfig,
                        knownNodeStorage: KnownNodeStorage): Behavior[DiscoveryRequest] = {

    import io.iohk.cef.encoding.rlp.RLPEncoders._
    import io.iohk.cef.encoding.rlp.RLPImplicits._

    val state = NodeState(
      key = node.id,
      serverStatus = ServerStatus.Listening(node.serverAddress),
      discoveryStatus = ServerStatus.NotListening,
      capabilities = node.capabilities
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
