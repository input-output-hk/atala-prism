package io.iohk.cef.network
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.time.Clock

import akka.actor.Scheduler
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.ActorContext
import akka.util.{ByteString, Timeout}
import io.iohk.cef.network.NodeStatus.NodeState
import io.iohk.cef.network.discovery.DiscoveryListener.DiscoveryListenerRequest
import io.iohk.cef.network.discovery.DiscoveryManager.DiscoveryRequest
import io.iohk.cef.network.discovery._
import io.iohk.cef.network.discovery.db.DummyKnownNodesStorage
import io.iohk.cef.network.encoding.nio._
import io.iohk.cef.network.encoding.{Decoder, Encoder}
import io.iohk.cef.network.telemetry.InMemoryTelemetry
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.network.transport.tcp.NetUtils.aRandomAddress
import io.iohk.cef.network.transport.tcp.{NetUtils, TcpTransportConfiguration}
import org.scalatest.FlatSpec
import org.scalatest.concurrent.Eventually._

import scala.collection.mutable

import org.mockito.Mockito.verify
import org.mockito.Mockito.atLeastOnce
import org.scalatest.mockito.MockitoSugar._
import scala.concurrent.duration._

class ConversationalNetworkIntegrationSpec extends FlatSpec {

  implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 5 seconds)

  object Messages {
    case class A(i: Int, s: String)
    case class B(s: String, b: Boolean)
  }

  import Messages._

  behavior of "ConversationalNetwork"

  it should "send a message to a peer" in {

    val bobsStack = randomBaseNetwork(None)
    val bobsNodeId = bobsStack.transports.peerInfo.nodeId

    val bobsAInbox = mockHandler[A]
    val bobA = messageChannel(bobsStack, bobsAInbox)

    val bobsBInbox = mockHandler[B]
    val bobB = messageChannel(bobsStack, bobsBInbox)

    val alicesStack = randomBaseNetwork(Some(bobsStack))
    val alicesNodeId = alicesStack.transports.peerInfo.nodeId

    val alicesAInbox = mockHandler[A]
    val aliceA = messageChannel(alicesStack, alicesAInbox)

    val alicesBInbox = mockHandler[B]
    val aliceB = messageChannel(alicesStack, alicesBInbox)

    eventually {
      aliceA.sendMessage(bobsNodeId, A(1, "Hi Bob!"))
      verify(bobsAInbox, atLeastOnce()).apply(alicesNodeId, A(1, "Hi Bob!"))
    }
  }

  private def mockHandler[T]: (NodeId, T) => Unit =
    mock[(NodeId, T) => Unit]

  private class MessageLog[T]() {
    val messages = mutable.ListBuffer[(NodeId, T)]()

    val messageHandler: (NodeId, T) => Unit = (nodeId, message) => messages += nodeId -> message
  }

  // Create a typed message channel on top of a base network instance
  private def messageChannel[T: NioEncoder: NioDecoder: Default](
      baseNetwork: BaseNetwork,
      messageHandler: (NodeId, T) => Unit): ConversationalNetwork[T] = {
    new ConversationalNetwork[T](messageHandler, baseNetwork.networkDiscovery, baseNetwork.transports)
  }

  // Each network node should have a single instance of the Transports and
  // a single instance of discovery.
  private class BaseNetwork(val transports: Transports, val networkDiscovery: NetworkDiscovery)

  private def randomBaseNetwork(bootstrap: Option[BaseNetwork]): BaseNetwork = {

    val configuration = ConversationalNetworkConfiguration(Some(TcpTransportConfiguration(aRandomAddress())))

    val peerInfo = PeerInfo(NodeId(NetUtils.randomBytes(NodeId.nodeIdBytes)), configuration)

    val transports = new Transports(peerInfo)

    val networkDiscovery: NetworkDiscovery = discovery(peerInfo, bootstrap.map(_.transports.peerInfo))

    new BaseNetwork(transports, networkDiscovery)
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

  private def discoveryManagerBehavior(peerInfo: PeerInfo,
                                       discoveryConfig: DiscoveryConfig): Behavior[DiscoveryRequest] = {

    val nodeInfo = peerInfo2NodeInfoHack(peerInfo)

    val nodeState = NodeState(nodeInfo.id,
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
      synchronousListenerFactory(discoveryConfig, encoder, decoder),
      new SecureRandom(),
      InMemoryTelemetry.registry
    )
    discoveryBehavior
  }

  private def synchronousListenerFactory(discoveryConfig: DiscoveryConfig,
                              encoder: Encoder[DiscoveryWireMessage, ByteString],
                              decoder: Decoder[ByteString, DiscoveryWireMessage])(
      context: ActorContext[DiscoveryRequest]): ActorRef[DiscoveryListenerRequest] = {

//    import akka.actor.typed.scaladsl.AskPattern._
    import scala.concurrent.duration._
    implicit val timeout: Timeout = 1 second
    implicit val scheduler: Scheduler = context.system.scheduler

    val discoveryListenerRef = context.spawn(
      DiscoveryListener.behavior(discoveryConfig, UDPBridge.creator(discoveryConfig, encoder, decoder)),
      "DiscoveryListener")

//    val futureStart = discoveryListenerRef ? Start
//
//    Await.result(futureStart, 1 minute)
    println("Discovery listener created.")
    discoveryListenerRef
  }

  private def peerInfo2NodeInfoHack(peerInfo: PeerInfo): NodeInfo = {
    val discoveryAddress =
      new InetSocketAddress("localhost", peerInfo.configuration.tcpTransportConfiguration.get.bindAddress.getPort + 1)

    val serverAddress = peerInfo.configuration.tcpTransportConfiguration.get.natAddress

    NodeInfo(peerInfo.nodeId.id, discoveryAddress, serverAddress, Capabilities(0))
  }

  private def clock(): Clock = Clock.systemUTC()
}
