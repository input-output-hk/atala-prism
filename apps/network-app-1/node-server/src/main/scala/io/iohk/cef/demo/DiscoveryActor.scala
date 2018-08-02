package io.iohk.cef.demo

import java.net.URI
import java.security.SecureRandom
import java.time.Clock

import akka.actor.typed.Behavior
import akka.util.ByteString
import io.iohk.cef.network.discovery.db.KnownNodeStorage
import io.iohk.cef.network.discovery.DiscoveryManager.DiscoveryRequest
import io.iohk.cef.network.discovery._
import io.iohk.cef.network.encoding.{Decoder, Encoder}
import io.iohk.cef.network.NodeStatus.NodeState
import io.iohk.cef.network.{NodeInfo, ServerStatus}
import io.iohk.cef.network.telemetry.DatadogTelemetry
import org.bouncycastle.util.encoders.Hex

object DiscoveryActor extends DatadogTelemetry {

  def discoveryBehavior(nodeInfo: NodeInfo,
                        discoveryConfig: DiscoveryConfig,
                        knownNodeStorage: KnownNodeStorage): Behavior[DiscoveryRequest] = {

    import io.iohk.cef.network.encoding.rlp.RLPEncoders._
    import io.iohk.cef.network.encoding.rlp.RLPImplicits._

    val state = NodeState(
      key = nodeInfo.id,
      serverStatus = ServerStatus.Listening(nodeInfo.serverAddress),
      discoveryStatus = ServerStatus.NotListening,
      capabilities = nodeInfo.capabilities
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
