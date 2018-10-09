package io.iohk.cef.network
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.time.Clock

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.ActorContext
import akka.util.ByteString
import io.iohk.cef.network.NodeStatus.NodeState
import io.iohk.cef.network.discovery.DiscoveryListener.DiscoveryListenerRequest
import io.iohk.cef.network.discovery.DiscoveryManager.DiscoveryRequest
import io.iohk.cef.network.discovery.db.DummyKnownNodesStorage
import io.iohk.cef.network.discovery._
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.telemetry.InMemoryTelemetry
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.network.transport.tcp.NetUtils.aRandomAddress
import io.iohk.cef.network.transport.tcp.{NetUtils, TcpTransportConfiguration}

trait NetworkFixture {

  // Each network node should have a single instance of the Transports and
  // a single instance of discovery.
  protected class BaseNetwork(val transports: Transports, val networkDiscovery: NetworkDiscovery)

  def randomBaseNetwork(bootstrap: Option[BaseNetwork]): BaseNetwork = {

    val configuration = NetworkConfiguration(Some(TcpTransportConfiguration(aRandomAddress())))

    val peerInfo = PeerInfo(NodeId(NetUtils.randomBytes(NodeId.nodeIdBytes)), configuration)

    val transports = new Transports(peerInfo)

    val networkDiscovery: NetworkDiscovery = discovery(peerInfo, bootstrap.map(_.transports.peerInfo))

    new BaseNetwork(transports, networkDiscovery)
  }

  def networks(fixtures: BaseNetwork*)(testCode: Seq[BaseNetwork] => Any): Unit = {
    try {
      testCode(fixtures)
    } finally {
      fixtures.foreach { fixture =>
        fixture.transports.shutdown()
        fixture.networkDiscovery.shutdown()
      }
    }
  }

  private def discovery(peerInfo: PeerInfo, bootstrapNode: Option[PeerInfo]): NetworkDiscovery = {

    import scala.concurrent.duration._

    val bootstrapNodes = bootstrapNode.map(peerInfo2NodeInfoHack).toSet

    val nodeInfo = peerInfo2NodeInfoHack(peerInfo)

    val discoveryConfig = DiscoveryConfig(
      discoveryEnabled = true,
      interface = "localhost",
      port = nodeInfo.discoveryAddress.getPort,
      bootstrapNodes = bootstrapNodes,
      discoveredNodesLimit = 100,
      scanNodesLimit = 100,
      concurrencyDegree = 100,
      scanInitialDelay = 0 millis,
      scanInterval = 1 minute,
      messageExpiration = 1 minute,
      maxSeekResults = 100,
      multipleConnectionsPerAddress = true,
      blacklistDefaultDuration = 1 minute
    )

    val discoveryBehavior: Behavior[DiscoveryRequest] =
      discoveryManagerBehavior(peerInfo, discoveryConfig)

    new DiscoveryManagerAdapter(discoveryBehavior)
  }

  private def discoveryManagerBehavior(
      peerInfo: PeerInfo,
      discoveryConfig: DiscoveryConfig): Behavior[DiscoveryRequest] = {

    val nodeInfo = peerInfo2NodeInfoHack(peerInfo)

    val nodeState = NodeState(
      nodeInfo.id,
      ServerStatus.Listening(nodeInfo.serverAddress),
      ServerStatus.Listening(nodeInfo.discoveryAddress),
      Capabilities(0))

    import io.iohk.cef.network.encoding.rlp.RLPEncoders._
    import io.iohk.cef.network.encoding.rlp.RLPImplicits._

    val encoder = implicitly[Encoder[DiscoveryWireMessage, ByteString]]

    val decoder = implicitly[Decoder[ByteString, DiscoveryWireMessage]]

    val discoveryBehavior = DiscoveryManager.behaviour(
      discoveryConfig,
      new DummyKnownNodesStorage(clock()) with InMemoryTelemetry,
      nodeState,
      clock(),
      encoder,
      decoder,
      listenerFactory(discoveryConfig, encoder, decoder),
      new SecureRandom(),
      InMemoryTelemetry.registry
    )
    discoveryBehavior
  }

  private def listenerFactory(
      discoveryConfig: DiscoveryConfig,
      encoder: Encoder[DiscoveryWireMessage, ByteString],
      decoder: Decoder[ByteString, DiscoveryWireMessage])(
      context: ActorContext[DiscoveryRequest]): ActorRef[DiscoveryListenerRequest] = {

    context.spawn(
      DiscoveryListener.behavior(discoveryConfig, UDPBridge.creator(discoveryConfig, encoder, decoder)),
      "DiscoveryListener")
  }

  // FIXME Get rid of NodeInfo
  private def peerInfo2NodeInfoHack(peerInfo: PeerInfo): NodeInfo = {
    val discoveryAddress =
      new InetSocketAddress("localhost", peerInfo.configuration.tcpTransportConfiguration.get.bindAddress.getPort + 1)

    val serverAddress = peerInfo.configuration.tcpTransportConfiguration.get.natAddress

    NodeInfo(peerInfo.nodeId.id, discoveryAddress, serverAddress, Capabilities(0))
  }

  private def clock(): Clock = Clock.systemUTC()
}
