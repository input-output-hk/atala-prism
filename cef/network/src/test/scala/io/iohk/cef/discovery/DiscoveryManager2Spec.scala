package io.iohk.cef.discovery

import java.net.{InetAddress, InetSocketAddress}
import java.security.SecureRandom

import akka.actor.typed.ActorRef._
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.{ActorSystem, typed}
import akka.testkit.{TestActors, TestProbe}
import akka.util.ByteString
import akka.{actor => untyped}
import io.iohk.cef.crypto
import io.iohk.cef.db.DummyKnownNodesStorage
import io.iohk.cef.discovery.DiscoveryListener.{Ready, SendMessage, Start}
import io.iohk.cef.discovery.DiscoveryManager2._
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.{Capabilities, Node, NodeStatus, ServerStatus}
import io.iohk.cef.test.TestClock
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import org.scalatest.MustMatchers._

import scala.concurrent.duration._

class DiscoveryManager2Spec extends WordSpecLike with BeforeAndAfterAll {

  val untypedSystem: ActorSystem = untyped.ActorSystem("TypedWatchingUntyped")
  val typedSystem: typed.ActorSystem[_] = untypedSystem.toTyped

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
        ByteString(0),
        ServerStatus.NotListening,
        ServerStatus.NotListening,
        Capabilities(0x01))

    import io.iohk.cef.encoding.rlp.RLPEncoders._
    import io.iohk.cef.encoding.rlp.RLPImplicits._

    val encoder = implicitly[Encoder[DiscoveryWireMessage, ByteString]]

    val decoder = implicitly[Decoder[ByteString, DiscoveryWireMessage]]

    val listener = TestProbe()(untypedSystem)

    val listenerProps = TestActors.forwardActorProps(listener.ref)

    val mockClock = new TestClock

    val listeningAddress = new InetSocketAddress(localhost,1000)

    val secureRandom = new SecureRandom()

    def createActor: ActorRef[TypedDiscoveryRequest] = {
      val behavior: Behavior[TypedDiscoveryRequest] = DiscoveryManager2.behaviour(
        discoveryConfig,
        new DummyKnownNodesStorage(mockClock),
        nodeState,
        mockClock,
        encoder, decoder,
        listenerProps, secureRandom)

      val actor: ActorRef[TypedDiscoveryRequest] =
        untypedSystem.spawn(behavior, s"ActorUnderTest_${randomAlphanumeric(5)}")

      actor ! TypedStartListening()

      listener.expectMsg(Start)
      listener.sender() ! Ready(discoveryAddress)

      actor
    }
  }

  private def getNode(listeningDiscoveryManager: ListeningDiscoveryManager): Node = {
    import listeningDiscoveryManager._
    Node(nodeState.nodeId, discoveryAddress, serverAddress, nodeState.capabilities)
  }

  private def getPing(listeningDiscoveryManager: ListeningDiscoveryManager): Ping = {
    import listeningDiscoveryManager._
    val expiration = mockClock.instant().getEpochSecond + 1
    val nonce = ByteString()
    Ping(DiscoveryWireMessage.ProtocolVersion, getNode(listeningDiscoveryManager), expiration, nonce)
  }

  private def pingActor(actor: ActorRef[TypedDiscoveryRequest],
                        listeningDiscoveryManager: ListeningDiscoveryManager): Ping = {
    val ping = getPing(listeningDiscoveryManager)
    actor ! MessageReceivedWrapper(DiscoveryListener.MessageReceived(ping, listeningDiscoveryManager.discoveryAddress))
    ping
  }

  "A DiscoveryManager" should {
    "initialize correctly" in new ListeningDiscoveryManager {
      createActor
    }
    "process a Ping message" in new ListeningDiscoveryManager {
      val actor: ActorRef[TypedDiscoveryRequest] = createActor

      val ping = pingActor(actor, this)

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
      val actor: ActorRef[TypedDiscoveryRequest] = createActor

      val node = getNode(this)

      actor ! TypedFetchNeighbors(node)
      val ping = listener.expectMsgType[DiscoveryListener.SendMessage].message.asInstanceOf[Ping]
      val token = calculateMessageKey(encoder, ping)

      val expiration = ping.timestamp
      val pong = Pong(node, token, expiration)
      val probe = TestProbe()(untypedSystem)
      untypedSystem.eventStream.subscribe(probe.ref, classOf[CompatibleNodeFound])
      actor ! MessageReceivedWrapper(DiscoveryListener.MessageReceived(pong, discoveryAddress))
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

      val ping = pingActor(actor, this)
      listener.expectMsgType[SendMessage].message.asInstanceOf[Pong]

      val node = Node(nodeState.nodeId,discoveryAddress, serverAddress, Capabilities(1))
      val expiration = mockClock.instant().getEpochSecond + 1
      val seek = Seek(Capabilities(1),10,expiration, ByteString())
      val token = crypto.kec256(encoder.encode(seek))

      actor ! MessageReceivedWrapper(DiscoveryListener.MessageReceived(seek, discoveryAddress))

      val sendMessage = listener.expectMsgType[DiscoveryListener.SendMessage]
      sendMessage.message mustBe a [Neighbors]
      sendMessage.message.messageType mustBe Neighbors.messageType
      sendMessage.message match {
        case neighbors: Neighbors =>
          neighbors.capabilities mustBe Capabilities(1)
          neighbors.neighbors mustBe Seq(node)
        case _ => fail("Wrong message type")
      }
    }
//    "process a Neighbors message" in new ListeningDiscoveryManager {
//      def createNode(id: String, discoveryPort: Int, serverPort: Int, capabilities: Capabilities) =
//        Node(ByteString(id),
//          new InetSocketAddress(localhost, discoveryPort),
//          new InetSocketAddress(localhost, serverPort),
//          capabilities)
//
//      val actor = createActor
//      val node = Node(nodeState.nodeId, discoveryAddress, serverAddress, nodeState.capabilities)
//      val nodeA = createNode("1", 9000, 9001, nodeState.capabilities)
//      val nodeB = createNode("2", 9003, 9002, nodeState.capabilities)
//      val nodeC = createNode("3", 9005, 9004, nodeState.capabilities)
//      val expiration = mockClock.instant().getEpochSecond + 2
//      val token = ByteString("token")
//      actor.underlyingActor.soughtNodes += ((token -> Sought(node, mockClock.instant())))
//      val neighbors = Neighbors(Capabilities(1), token, 10, Seq(nodeA, nodeB, nodeC), expiration)
//      mockClock.tick
//      actor ! DiscoveryListener.MessageReceived(neighbors, discoveryAddress)
//
//      val pingA = listener.expectMsgType[DiscoveryListener.SendMessage]
//      val pingB = listener.expectMsgType[DiscoveryListener.SendMessage]
//
//      actor.underlyingActor.pingedNodes.values.toSet mustBe Set(Pinged(nodeA, mockClock.instant), Pinged(nodeB, mockClock.instant))
//      pingA.message mustBe a [Ping]
//      pingB.message mustBe a [Ping]
//      pingA.to mustBe nodeA.discoveryAddress
//      pingB.to mustBe nodeB.discoveryAddress
//
//      (pingA.message, pingB.message) match {
//        case (pa: Ping, pb: Ping) =>
//          pa.messageType mustBe Ping.messageType
//          pb.messageType mustBe Ping.messageType
//        case _ => fail("Wrong message type")
//      }
//    }
//    "discover the peers of a connected node" in {
//      pending
//    }
//    "stop accepting new peers when the nodes limit is reached" in {
//      pending
//    }
//    "scan pinged nodes and already discovered nodes" in {
//      pending
//    }
//    "prevent multiple connections from the same IP when configured" in {
//      pending
//    }
//    "not process pong messages in absence of a ping" in new ListeningDiscoveryManager {
//      val actor = createActor
//      val node = Node(nodeState.nodeId,discoveryAddress, serverAddress, nodeState.capabilities)
//      val expiration = mockClock.instant().getEpochSecond + 1
//      val pong = Pong(node,ByteString("token"),expiration)
//      val probe = TestProbe()
//      system.eventStream.subscribe(probe.ref, classOf[CompatibleNodeFound])
//      actor ! DiscoveryListener.MessageReceived(pong, discoveryAddress)
//      probe.expectNoMessage()
//    }
//    "not process neighbors messages in absence of a seek" in {
//      pending
//    }
  }

  override protected def afterAll(): Unit = {
    typedSystem.terminate()
    untypedSystem.terminate()
  }
}
