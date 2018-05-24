package io.iohk.cef

import java.net.{InetSocketAddress, URI}
import java.security.SecureRandom
import java.time.Clock

import akka.util.ByteString
import akka.{actor => untyped}
import io.iohk.cef.db.DummyKnownNodesStorage
import io.iohk.cef.discovery._
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.NodeStatus.NodeState
import io.iohk.cef.network.{Capabilities, Node, NodeStatus, ServerStatus}
import org.bouncycastle.util.encoders.Hex

import scala.concurrent.duration._

object DiscoveryActor {

  def apply(uri: URI, bootstrapNodeUris: Set[URI], capabilities: Capabilities)(context: untyped.ActorContext):
    untyped.ActorRef = {

    import io.iohk.cef.encoding.rlp.RLPEncoders._
    import io.iohk.cef.encoding.rlp.RLPImplicits._
    import akka.actor.typed.scaladsl.adapter._

    val state = NodeState(
      key = toNodeKey(uri),
      serverStatus = ServerStatus.Listening(localhost(uri.getPort)),
      discoveryStatus = ServerStatus.NotListening,
      capabilities = capabilities)

    val discoveryConfig = config(uri, bootstrapNodeUris, capabilities)

    val stateHolder = context.spawn(NodeStatus.nodeState(state, Seq()), "stateHolder")

    val encoder = implicitly[Encoder[DiscoveryWireMessage, ByteString]]

    val decoder = implicitly[Decoder[ByteString, DiscoveryWireMessage]]

    val discoveryManagerProps = DiscoveryManager.props(
      discoveryConfig,
      new DummyKnownNodesStorage(Clock.systemUTC()),
      stateHolder,
      Clock.systemUTC(),
      encoder,
      decoder,
      DiscoveryManager.listenerMaker(discoveryConfig, stateHolder, encoder, decoder),
      context.system.scheduler,
      new SecureRandom()
    )

    context.actorOf(discoveryManagerProps)
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
      multipleConnectionsPerAddress = true
    )
  }

  private def discoveryPort(serverPort: Int): Int = serverPort + 1

  private def toNodeKey(nodeUri: URI): ByteString =
    ByteString(Hex.decode(nodeUri.getUserInfo))
}
