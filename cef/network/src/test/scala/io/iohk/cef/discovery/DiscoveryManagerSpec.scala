package io.iohk.cef.discovery

import java.net.{InetAddress, InetSocketAddress}
import java.security.SecureRandom

import akka.actor.typed.scaladsl.adapter._
import akka.testkit.typed.scaladsl.TestInbox
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import akka.util.ByteString
import akka.{actor => untyped}
import io.iohk.cef.db.KnownNodesStorage
import io.iohk.cef.discovery.DiscoveryListener.Start
import io.iohk.cef.discovery.DiscoveryManager.{Pinged, Sought}
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.NodeStatus.{NodeStatusMessage, Subscribe}
import io.iohk.cef.network.{Capabilities, Node, NodeStatus, ServerStatus}
import io.iohk.cef.test.{StopAfterAll, TestClock}
import io.iohk.cef.{crypto, network}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpecLike}

import scala.concurrent.duration._

class DiscoveryManagerSpec extends TestKit(untyped.ActorSystem("DiscoveryManagerSpec"))
  with WordSpecLike
  with StopAfterAll
  with MustMatchers
  with MockFactory {

  trait ListeningDiscoveryManager {

    val address: Array[Byte] = Array(127.toByte,0,0,1)
    val localhost = InetAddress.getByAddress("",address)
    val discoveryAddress = new InetSocketAddress(localhost, 1000)
    val serverAddress = new InetSocketAddress(localhost, 2000)

    def bootstrapNodes: Set[Node] = Set()

    def discoveryConfig = new DiscoveryConfig(
      discoveryEnabled = true,
      interface = "0.0.0.0",
      port = 8090,
      bootstrapNodes = bootstrapNodes,
      discoveredNodesLimit = 10,
      scanNodesLimit = 10,
      concurrencyDegree = 10,
      scanInitialDelay = 10.minutes,
      scanInterval = 11.minutes,
      messageExpiration = 100.minute,
      maxSeekResults = 10,
      multipleConnectionsPerAddress = true)
    val nodeState =
      NodeStatus.NodeState(
        network.loadAsymmetricCipherKeyPair("/tmp/file", SecureRandom.getInstance("NativePRNGNonBlocking")),
        ServerStatus.NotListening,
        ServerStatus.NotListening,
        Capabilities(0x01))

    import io.iohk.cef.encoding.rlp.RLPEncoders._
    import io.iohk.cef.encoding.rlp.RLPImplicits._

    val encoder = implicitly[Encoder[DiscoveryWireMessage, ByteString]]

    val decoder = implicitly[Decoder[ByteString, DiscoveryWireMessage]]

    val listener = TestProbe()

    val listenerMaker = (_: untyped.ActorRefFactory) => listener.ref

    val mockClock = new TestClock

    val listeningAddress = new InetSocketAddress(localhost,1000)

    val scheduler = system.scheduler

    val nodeStateInbox = TestInbox[NodeStatusMessage]()

    val secureRandom = new SecureRandom()

    def createActor = {
      val actor = TestActorRef[DiscoveryManager](
        DiscoveryManager.props(
          discoveryConfig,
          new KnownNodesStorage(mockClock),
          nodeStateInbox.ref,
          mockClock,
          encoder,
          decoder,
          listenerMaker,
          scheduler,
          secureRandom
        )
      )
      nodeStateInbox.expectMessage(Subscribe(actor))
      actor ! NodeStatus.StateUpdated(nodeState)
      listener.expectMsg(Start)
      actor ! DiscoveryListener.Ready(listeningAddress)
      actor
    }
  }


  "A DiscoveryManager" should {
    "initialize correctly" in new ListeningDiscoveryManager {
      val actor = createActor
      actor.underlyingActor.pingedNodes.values.size mustBe 0
      actor.underlyingActor.soughtNodes.values.size mustBe 0
    }
    "process a Ping message" in new ListeningDiscoveryManager {
      val actor = createActor
      val expiration = mockClock.instant().getEpochSecond + 1
      val nonce = ByteString()
      val node = Node(nodeState.nodeId, discoveryAddress, serverAddress, nodeState.capabilities)
      val ping = Ping(DiscoveryWireMessage.ProtocolVersion, node, expiration, nonce)
      actor ! DiscoveryListener.MessageReceived(ping, discoveryAddress)
      val token = crypto.kec256(encoder.encode(ping))
      val sendMessage = listener.expectMsgType[DiscoveryListener.SendMessage]
      sendMessage.message mustBe a [Pong]
      sendMessage.message.messageType mustBe Pong.messageType
      sendMessage.message match {
        case pong: Pong =>
          pong.node.capabilities mustBe nodeState.capabilities
          pong.token mustBe token
        case _ => fail("Wrong message type")
      }
    }
    "process a Pong message" in new ListeningDiscoveryManager {
      val actor = createActor
      val token = ByteString("token")
      val node = Node(nodeState.nodeId,discoveryAddress, serverAddress,nodeState.capabilities)
      actor.underlyingActor.pingedNodes += ((token -> Pinged(node, mockClock.instant())))
      val expiration = mockClock.instant().getEpochSecond + 1
      val pong = Pong(node,token,expiration)
      val probe = TestProbe()
      system.eventStream.subscribe(probe.ref, classOf[CompatibleNodeFound])
      actor ! DiscoveryListener.MessageReceived(pong, discoveryAddress)
      probe.expectMsg(CompatibleNodeFound(node))
      val sendMessage = listener.expectMsgType[DiscoveryListener.SendMessage]
      sendMessage.message mustBe a [Seek]
      sendMessage.message.messageType mustBe Seek.messageType
      sendMessage.message match {
        case seek: Seek =>
          seek.capabilities mustBe nodeState.capabilities
        case _ => fail("Wrong message type")
      }
    }
    "process a Seek message" in new ListeningDiscoveryManager {
      val actor = createActor
      val node = Node(nodeState.nodeId,discoveryAddress, serverAddress, Capabilities(11))
      //actor.underlyingActor.discoveredNodes = actor.underlyingActor.discoveredNodes :+ Discovered(node, mockClock.instant())
      val expiration = mockClock.instant().getEpochSecond + 1
      val seek = Seek(Capabilities(10),10,expiration, ByteString())
      val token = crypto.kec256(encoder.encode(seek))
      actor.underlyingActor.soughtNodes += ((token -> Sought(node, mockClock.instant())))
      actor ! DiscoveryListener.MessageReceived(seek, discoveryAddress)

      val sendMessage = listener.expectMsgType[DiscoveryListener.SendMessage]
      sendMessage.message mustBe a [Neighbors]
      sendMessage.message.messageType mustBe Neighbors.messageType
      sendMessage.message match {
        case neighbors: Neighbors =>
          neighbors.capabilities mustBe Capabilities(10)
          neighbors.neighbors mustBe Seq(node)
        case _ => fail("Wrong message type")
      }
    }
    "process a Neighbors message" in new ListeningDiscoveryManager {
      def createNode(id: String, discoveryPort: Int, serverPort: Int, capabilities: Capabilities) =
        Node(ByteString(id),
          new InetSocketAddress(localhost, discoveryPort),
          new InetSocketAddress(localhost, serverPort),
          capabilities)

      val actor = createActor
      val node = Node(nodeState.nodeId, discoveryAddress, serverAddress, nodeState.capabilities)
      val nodeA = createNode("1", 9000, 9001, nodeState.capabilities)
      val nodeB = createNode("2", 9003, 9002, nodeState.capabilities)
      val nodeC = createNode("3", 9005, 9004, nodeState.capabilities)
      val expiration = mockClock.instant().getEpochSecond + 2
      val token = ByteString("token")
      actor.underlyingActor.soughtNodes += ((token -> Sought(node, mockClock.instant())))
      val neighbors = Neighbors(Capabilities(1), token, 10, Seq(nodeA, nodeB, nodeC), expiration)
      mockClock.tick
      actor ! DiscoveryListener.MessageReceived(neighbors, discoveryAddress)

      val pingA = listener.expectMsgType[DiscoveryListener.SendMessage]
      val pingB = listener.expectMsgType[DiscoveryListener.SendMessage]

      actor.underlyingActor.pingedNodes.values.toSet mustBe Set(Pinged(nodeA, mockClock.instant), Pinged(nodeB, mockClock.instant))
      pingA.message mustBe a [Ping]
      pingB.message mustBe a [Ping]
      pingA.to mustBe nodeA.discoveryAddress
      pingB.to mustBe nodeB.discoveryAddress

      (pingA.message, pingB.message) match {
        case (pa: Ping, pb: Ping) =>
          pa.messageType mustBe Ping.messageType
          pb.messageType mustBe Ping.messageType
        case _ => fail("Wrong message type")
      }
    }
    "discover the peers of a connected node" in {
      pending
    }
    "stop accepting new peers when the nodes limit is reached" in {
      pending
    }
    "scan pinged nodes and already discovered nodes" in {
      pending
    }
    "prevent multiple connections from the same IP when configured" in {
      pending
    }
    "not process pong messages in absence of a ping" in new ListeningDiscoveryManager {
      val actor = createActor
      val node = Node(nodeState.nodeId,discoveryAddress, serverAddress, nodeState.capabilities)
      val expiration = mockClock.instant().getEpochSecond + 1
      val pong = Pong(node,ByteString("token"),expiration)
      val probe = TestProbe()
      system.eventStream.subscribe(probe.ref, classOf[CompatibleNodeFound])
      actor ! DiscoveryListener.MessageReceived(pong, discoveryAddress)
      probe.expectNoMessage()
    }
    "not process neighbors messages in absence of a seek" in {
      pending
    }
  }
}
