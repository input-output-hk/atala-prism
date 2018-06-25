package io.iohk.cef.discovery

import java.net.{InetAddress, InetSocketAddress}
import java.security.SecureRandom

import akka.actor.typed.ActorRef._
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorContext, ActorRef}
import akka.actor.{ActorSystem, typed}
import akka.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox, TestProbe}
import akka.testkit.{TestProbe => UntypedTestProbe}
import akka.util.ByteString
import akka.{actor => untyped}
import io.iohk.cef.crypto
import io.iohk.cef.db.{AutoRollbackSpec, DummyKnownNodesStorage, KnownNode}
import io.iohk.cef.discovery.DiscoveryListener.{DiscoveryListenerRequest, Ready, SendMessage, Start}
import io.iohk.cef.discovery.DiscoveryManager._
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.{Capabilities, Node, NodeStatus, ServerStatus}
import io.iohk.cef.test.TestClock
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.apache.commons.lang3.RandomStringUtils.{randomAlphabetic, randomAlphanumeric}
import org.scalatest.MustMatchers._
import org.scalatest.{BeforeAndAfterAll, fixture}
import scalikejdbc.DBSession

import scala.concurrent.duration._

class DiscoveryManagerSpec extends fixture.FlatSpecLike with AutoRollbackSpec with BeforeAndAfterAll {

  implicit val untypedSystem: ActorSystem = untyped.ActorSystem("TypedWatchingUntyped")
  implicit val typedSystem: typed.ActorSystem[_] = untypedSystem.toTyped

  trait ListeningDiscoveryManager {

    val session: DBSession

    val mockClock = new TestClock

    val knownNodeStorage = new DummyKnownNodesStorage(mockClock)

    val address: Array[Byte] = Array(127.toByte,0,0,1)
    val localhost = InetAddress.getByAddress("",address)
    val discoveryAddress = new InetSocketAddress(localhost, 1000)
    val serverAddress = new InetSocketAddress(localhost, 2000)

    val nodeState =
      NodeStatus.NodeState(
        ByteString(0),
        ServerStatus.NotListening,
        ServerStatus.NotListening,
        Capabilities(0x01))

    val nodeA = createNode("1", 9000, 9001, nodeState.capabilities)
    val nodeB = createNode("2", 9003, 9002, nodeState.capabilities)

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
      multipleConnectionsPerAddress = true,
      blacklistDefaultDuration = 30 seconds)

    import io.iohk.cef.encoding.rlp.RLPEncoders._
    import io.iohk.cef.encoding.rlp.RLPImplicits._

    val encoder = implicitly[Encoder[DiscoveryWireMessage, ByteString]]

    val decoder = implicitly[Decoder[ByteString, DiscoveryWireMessage]]

    val discoveryListener = TestProbe[DiscoveryListenerRequest]

    val listenerMaker: ActorContext[DiscoveryRequest] => ActorRef[DiscoveryListenerRequest] = _ => discoveryListener.ref

    val listeningAddress = new InetSocketAddress(localhost,1000)

    val secureRandom = new SecureRandom()

    def createBehavior = DiscoveryManager.behaviour(
      discoveryConfig,
      knownNodeStorage,
      nodeState,
      mockClock,
      encoder, decoder,
      listenerMaker,
      secureRandom,
      new SimpleMeterRegistry())

    def createActor: ActorRef[DiscoveryRequest] = {
      val behavior = createBehavior

      val actor: ActorRef[DiscoveryRequest] =
        untypedSystem.spawn(behavior, s"ActorUnderTest_${randomAlphanumeric(5)}")

      val startMessage = discoveryListener.expectMessageType[Start]
      startMessage.replyTo ! Ready(discoveryAddress)

      actor
    }

    def createNode(id: String, discoveryPort: Int, serverPort: Int, capabilities: Capabilities) =
      Node(ByteString(id),
        new InetSocketAddress(localhost, discoveryPort),
        new InetSocketAddress(localhost, serverPort),
        capabilities)
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

  def pingActor(actor: ActorRef[DiscoveryRequest],
                        listeningDiscoveryManager: ListeningDiscoveryManager): Ping = {
    val ping = getPing(listeningDiscoveryManager)
    actor ! DiscoveryResponseWrapper(DiscoveryListener.MessageReceived(ping, listeningDiscoveryManager.discoveryAddress))
    ping
  }

  "A DiscoveryManager" should "initialize correctly" in { s =>
    new ListeningDiscoveryManager {
      override val session: DBSession = s
      createActor
    }
  }
  it should "process a Ping message" in { s =>
    new ListeningDiscoveryManager {
      override val session: DBSession = s
      val actor: ActorRef[DiscoveryRequest] = createActor

      val ping = pingActor(actor, this)

      val token = crypto.kec256(encoder.encode(ping))
      val sendMessage = discoveryListener.expectMessageType[SendMessage]
      sendMessage.message mustBe a[Pong]
      sendMessage.message.messageType mustBe Pong.messageType
      sendMessage.message match {
        case pong: Pong =>
          pong.node.capabilities mustBe nodeState.capabilities
          pong.token mustBe token
        case _ => fail("Wrong message type")
      }
    }
  }
  it should "process a Pong message" in { s =>
    new ListeningDiscoveryManager {
      override val session: DBSession = s
      val actor: ActorRef[DiscoveryRequest] = createActor

      val node = getNode(this)

      actor ! FetchNeighbors(node)
      val ping = discoveryListener.expectMessageType[SendMessage].message.asInstanceOf[Ping]
      val token = calculateMessageKey(encoder, ping)

      val expiration = ping.timestamp
      val pong = Pong(node, token, expiration)
      val probe = UntypedTestProbe(randomAlphabetic(6))(untypedSystem)
      untypedSystem.eventStream.subscribe(probe.ref, classOf[CompatibleNodeFound])
      actor ! DiscoveryResponseWrapper(DiscoveryListener.MessageReceived(pong, discoveryAddress))
      probe.expectMsg(CompatibleNodeFound(node))
      val sendMessage = discoveryListener.expectMessageType[SendMessage]
      sendMessage.message mustBe a[Seek]
      sendMessage.message.messageType mustBe Seek.messageType
      sendMessage.message match {
        case seek: Seek =>
          seek.capabilities mustBe nodeState.capabilities
        case _ => fail("Wrong message type")
      }
    }
  }
  it should "process a Seek message" in { s =>
    new ListeningDiscoveryManager {
      override val session: DBSession = s
      val actor = createActor

      val ping = pingActor(actor, this)
      discoveryListener.expectMessageType[SendMessage].message.asInstanceOf[Pong]

      val node = Node(nodeState.nodeId, discoveryAddress, serverAddress, Capabilities(1))
      val expiration = mockClock.instant().getEpochSecond + 1
      val seek = Seek(Capabilities(1), 10, expiration, ByteString())
      val token = crypto.kec256(encoder.encode(seek))

      actor ! DiscoveryResponseWrapper(DiscoveryListener.MessageReceived(seek, discoveryAddress))

      val sendMessage = discoveryListener.expectMessageType[SendMessage]
      sendMessage.message mustBe a[Neighbors]
      sendMessage.message.messageType mustBe Neighbors.messageType
      sendMessage.message match {
        case neighbors: Neighbors =>
          neighbors.capabilities mustBe Capabilities(1)
          neighbors.neighbors mustBe Seq(node)
        case _ => fail("Wrong message type")
      }
    }
  }
  it should "discover the peers of a connected node" in { s =>
    pending
    new ListeningDiscoveryManager {
      override val session = s

      val actor = createActor
      val node = Node(nodeState.nodeId, discoveryAddress, serverAddress, nodeState.capabilities)
      val expiration = mockClock.instant().getEpochSecond + 2
      mockClock.tick

      actor ! DiscoveryResponseWrapper(Ready(new InetSocketAddress(localhost, 9000)))
      discoveryListener.expectMessageType[Start](1 seconds)
      actor ! FetchNeighbors(nodeA)
      val pingA = discoveryListener.expectMessageType[DiscoveryListener.SendMessage](5 second)
      pingA.message mustBe a[Ping]
      pingA.to mustBe nodeA.discoveryAddress
      val pongA = Pong(nodeA, calculateMessageKey(encoder, pingA.message), mockClock.instant().getEpochSecond)
      actor ! DiscoveryResponseWrapper(DiscoveryListener.MessageReceived(pongA, discoveryAddress))
      val seek = discoveryListener.expectMessageType[DiscoveryListener.SendMessage](1 second)
      seek.message mustBe a [Neighbors]
      seek.to mustBe nodeA.discoveryAddress
      val neighbors = Neighbors(Capabilities(1), calculateMessageKey(encoder, seek.message), 10, Seq(nodeB), expiration)
      actor ! DiscoveryResponseWrapper(DiscoveryListener.MessageReceived(neighbors, discoveryAddress))
      val pingB = discoveryListener.expectMessageType[DiscoveryListener.SendMessage](1 second)
      val pingC = discoveryListener.expectMessageType[DiscoveryListener.SendMessage](1 second)

      pingB.message mustBe a[Ping]
      pingB.to mustBe nodeB.discoveryAddress
      pingC.message mustBe a[Ping]
      pingC.to mustBe nodeB.discoveryAddress

      (pingB.message, pingC.message) match {
        case (pb: Ping, pc: Ping) =>
          pb.messageType mustBe Ping.messageType
          pc.messageType mustBe Ping.messageType
        case _ => fail("Wrong message type")
      }
    }
  }
  it should "ping bootstrap nodes" in { s =>
    new ListeningDiscoveryManager {
      override val session: FixtureParam = s

      override def bootstrapNodes: Set[Node] = {
        Set(nodeA, nodeB)
      }

      val actor = createActor
      val pingA = discoveryListener.expectMessageType[DiscoveryListener.SendMessage]
      val pingB = discoveryListener.expectMessageType[DiscoveryListener.SendMessage]
      pingA.message mustBe a [Ping]
      pingB.message mustBe a [Ping]
      pingA.message.messageType mustBe Ping.messageType
      pingB.message.messageType mustBe Ping.messageType
      (pingA.message, pingB.message) match {
        case (a: Ping, b: Ping) =>
          val thisNode = Node(nodeState.nodeId, discoveryAddress, discoveryAddress, Capabilities(1))
          a.node mustBe thisNode
          b.node mustBe thisNode
          val addresses = bootstrapNodes.map(_.discoveryAddress)
          addresses must contain (pingA.to)
          addresses must contain(pingB.to)
          pingA.to must not be pingB.to
        case _ => fail("Wrong message type")
      }
    }
  }
  it should "return the known nodes" in {s =>
    new ListeningDiscoveryManager {
      override val session: FixtureParam = s

      val actor = createActor

      knownNodeStorage.insert(nodeA)
      knownNodeStorage.insert(nodeB)

      val inbox = TestProbe[DiscoveredNodes]()

      actor ! GetDiscoveredNodes(inbox.ref)

      inbox.expectMessage(DiscoveredNodes(
        Set(KnownNode(nodeA, mockClock.instant(), mockClock.instant()),
          KnownNode(nodeB, mockClock.instant(), mockClock.instant())))
      )
    }
  }
  it should "blacklist nodes" in { s =>
    new ListeningDiscoveryManager {
      override val session: FixtureParam = s

      val blacklistDuration = 1 second

      override def discoveryConfig: DiscoveryConfig =
        super.discoveryConfig.copy(
          blacklistDefaultDuration = blacklistDuration
        )

      val actor = createActor

      knownNodeStorage.insert(nodeA)

      val probe = TestProbe[DiscoveredNodes]()

      actor ! GetDiscoveredNodes(probe.ref)

      probe.expectMessageType[DiscoveredNodes].nodes.size mustBe 1

      actor ! Blacklist(nodeA)
      actor ! GetDiscoveredNodes(probe.ref)

      probe.expectMessageType[DiscoveredNodes].nodes.size mustBe 0
    }
  }
  it should "stop accepting new peers when the nodes limit is reached" in { s =>
    pending
  }
  it should "scan pinged nodes and already discovered nodes" in { s =>
    pending
  }
  it should "prevent multiple connections from the same IP when configured" in { s =>
    pending
  }
  it should "not process pong messages in absence of a ping" in { s =>
    new ListeningDiscoveryManager {
      override val session = s
      val inbox = TestInbox[DiscoveryListenerRequest]()
      override val listenerMaker = (_: ActorContext[DiscoveryRequest]) => inbox.ref

      val actor = BehaviorTestKit(createBehavior)
      val node = Node(nodeState.nodeId, discoveryAddress, serverAddress, nodeState.capabilities)
      val expiration = mockClock.instant().getEpochSecond + 1
      val pong = Pong(node, ByteString("token"), expiration)
      actor.run(DiscoveryResponseWrapper(DiscoveryListener.MessageReceived(pong, discoveryAddress)))
      knownNodeStorage.getAll() mustBe Set()
      inbox.receiveAll().collect { case w: DiscoveryResponseWrapper => w} mustBe Seq()
    }
  }
  it should "not process neighbors messages in absence of a seek" in { s =>
    new ListeningDiscoveryManager {
      override val session = s

      val actor = createActor
      val node = Node(nodeState.nodeId, discoveryAddress, serverAddress, nodeState.capabilities)
      val expiration = mockClock.instant().getEpochSecond + 2
      val token = ByteString("token")
      val neighbors = Neighbors(Capabilities(1), token, 10, Seq(nodeA), expiration)
      mockClock.tick

      actor ! DiscoveryResponseWrapper(Ready(new InetSocketAddress(localhost, 9000)))
      actor ! DiscoveryResponseWrapper(DiscoveryListener.MessageReceived(neighbors, discoveryAddress))
      discoveryListener.expectMessageType[Start](1 second)
      val pingB = discoveryListener.expectNoMessage(1 second)
    }
  }

  override protected def afterAll(): Unit = {
    typedSystem.terminate()
    untypedSystem.terminate()
  }
}
