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
import io.iohk.cef.discovery.DiscoveryManager.{Discovered, Pinged}
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.NodeStatus.{NodeStatusMessage, Subscribe}
import io.iohk.cef.network.{Capabilities, Endpoint, Node, NodeAddress, NodeStatus, ServerStatus}
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
    val localhostEndpoint = Endpoint(localhost, 1000, 2000)

    def bootstrapNodes: Set[Node] = Set()

    def discoveryConfig = new DiscoveryConfig(
      discoveryEnabled = true,
      interface = "0.0.0.0",
      port = 8090,
      bootstrapNodes = bootstrapNodes,
      nodesLimit = 10,
      scanMaxNodes = 10,
      scanInitialDelay = 10.minutes,
      scanInterval = 11.minutes,
      messageExpiration = 100.minute,
      maxSeekResults = 10,
      multipleConnectionsPerAddress = true)
    val nodeState =
      NodeStatus.NodeState(
        network.loadAsymmetricCipherKeyPair("/tmp/file", SecureRandom.getInstance("NativePRNGNonBlocking")),
        ServerStatus.NotListening,
        Capabilities(0x01))

    import io.iohk.cef.encoding.rlp.RLPEncoders._
    import io.iohk.cef.encoding.rlp.RLPImplicits._

    val encoder = implicitly[Encoder[DiscoveryMessage, ByteString]]

    val decoder = implicitly[Decoder[ByteString, DiscoveryMessage]]

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
          new KnownNodesStorage,
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
      actor.underlyingActor.pingedNodes.size mustBe 0
      actor.underlyingActor.discoveredNodes.size mustBe 0
      actor.underlyingActor.soughtNodes.size mustBe 0
    }
    "process a Ping message" in new ListeningDiscoveryManager {
      val actor = createActor
      val expiration = mockClock.instant().getEpochSecond + 1
      val nonce = Array[Byte]()
      val ping = Ping(DiscoveryMessage.ProtocolVersion, localhostEndpoint, expiration, nonce)
      actor ! DiscoveryListener.MessageReceived(ping, localhostEndpoint.toUdpAddress)
      val token = crypto.kec256(encoder.encode(ping))
      val sendMessage = listener.expectMsgType[DiscoveryListener.SendMessage]
      sendMessage.message mustBe a [Pong]
      sendMessage.message.messageType mustBe Pong.messageType
      sendMessage.message match {
        case pong: Pong =>
          pong.capabilities mustBe nodeState.capabilities
          pong.token mustBe token
        case _ => fail("Wrong message type")
      }
    }
    "process a Pong message" in new ListeningDiscoveryManager {
      val actor = createActor
      val token = ByteString("token")
      val node = Node(ByteString("1"),NodeAddress(InetAddress.getLocalHost, 9000, 9001),Capabilities(1))
      actor.underlyingActor.pingedNodes += ((token -> Pinged(node, mockClock.instant())))
      val expiration = mockClock.instant().getEpochSecond + 1
      val pong = Pong(nodeState.capabilities,token,expiration)
      val probe = TestProbe()
      system.eventStream.subscribe(probe.ref, classOf[CompatibleNodeFound])
      actor ! DiscoveryListener.MessageReceived(pong, localhostEndpoint.toUdpAddress)
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
      val node = Node(ByteString("1"),NodeAddress(InetAddress.getLocalHost, 9000, 9001),Capabilities(11))
      actor.underlyingActor.discoveredNodes = actor.underlyingActor.discoveredNodes :+ Discovered(node, mockClock.instant())
      val expiration = mockClock.instant().getEpochSecond + 1
      val seek = Seek(Capabilities(10),10,expiration, Array[Byte]())
      val token = crypto.kec256(encoder.encode(seek))
      actor.underlyingActor.soughtNodes += ((token -> 10))
      actor ! DiscoveryListener.MessageReceived(seek, localhostEndpoint.toUdpAddress)

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
      val actor = createActor
      val nodeA = Node(ByteString("1"),NodeAddress(InetAddress.getLocalHost, 9000, 9001),Capabilities(1))
      val nodeB = Node(ByteString("2"),NodeAddress(InetAddress.getLocalHost, 9002, 9003),Capabilities(1))
      val nodeC = Node(ByteString("3"),NodeAddress(InetAddress.getLocalHost, 9004, 9005),Capabilities(1))
      val expiration = mockClock.instant().getEpochSecond + 2
      val token = ByteString("token")
      actor.underlyingActor.soughtNodes += ((token -> 2))
      val neighbors = Neighbors(Capabilities(1), token, 10, Seq(nodeA, nodeB, nodeC), expiration)
      mockClock.tick
      actor ! DiscoveryListener.MessageReceived(neighbors, localhostEndpoint.toUdpAddress)

      val pingA = listener.expectMsgType[DiscoveryListener.SendMessage]
      val pingB = listener.expectMsgType[DiscoveryListener.SendMessage]

      actor.underlyingActor.pingedNodes.values.toSet mustBe Set(Pinged(nodeA, mockClock.instant), Pinged(nodeB, mockClock.instant))
      pingA.message mustBe a [Ping]
      pingB.message mustBe a [Ping]
      pingA.to mustBe nodeA.address.udpSocketAddress
      pingB.to mustBe nodeB.address.udpSocketAddress

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
      val node = Node(ByteString("1"),NodeAddress(InetAddress.getLocalHost, 9000, 9001),Capabilities(1))
      val expiration = mockClock.instant().getEpochSecond + 1
      val pong = Pong(nodeState.capabilities,ByteString("token"),expiration)
      val probe = TestProbe()
      system.eventStream.subscribe(probe.ref, classOf[CompatibleNodeFound])
      actor ! DiscoveryListener.MessageReceived(pong, localhostEndpoint.toUdpAddress)
      probe.expectNoMessage()
    }
    "not process neighbors messages in absence of a seek" in {
      pending
    }
  }
}
