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

    def bootstrapNodes: Set[Node] = Set(Node(ByteString("1"),NodeAddress(localhost,0,8091), Capabilities(0x0)))

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

    //Not sure how to mock parameterless methods.
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
      override def bootstrapNodes: Set[Node] = Set()
      val actor = createActor
      actor.underlyingActor.pingedNodes.size mustBe 0
      actor.underlyingActor.discoveredNodes.size mustBe 0
      actor.underlyingActor.soughtNodes.size mustBe 0
    }
    "process a Ping message" in new ListeningDiscoveryManager {

      override def bootstrapNodes: Set[Node] = Set()

      //override val
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
      pending
    }
    "process a Seek message" in {
      pending
    }
    "process a Neighbors message" in {
      pending
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
    "not process pong messages in absence of a ping" in {
      pending
    }
    "not process neighbors messages in absence of a seek" in {
      pending
    }
  }
}
