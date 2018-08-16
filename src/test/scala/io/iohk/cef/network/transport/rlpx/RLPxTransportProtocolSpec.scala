package io.iohk.cef.network.transport.rlpx

import java.net.{InetSocketAddress, URI}

import akka.actor.{ActorSystem, typed}
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, Behavior}
import akka.io.Tcp.{Bind, Bound, CommandFailed}
import akka.testkit.typed.scaladsl.TestProbe
import akka.testkit.{TestActors, TestProbe => UntypedTestProbe}
import akka.util.ByteString
import akka.{actor => untyped}
import io.iohk.cef.network.transport.rlpx.RLPxConnectionHandler.{ConnectTo, ConnectionEstablished, ConnectionFailed, HandleConnection}
import io.iohk.cef.test.TestEncoderDecoder
import io.iohk.cef.test.TestEncoderDecoder.TestMessage
import io.iohk.cef.test.TypedTestProbeOps._
import org.bouncycastle.util.encoders.Hex
import org.scalatest.{Assertion, BeforeAndAfterAll, FlatSpec}
import org.scalatest.Matchers._

import scala.concurrent.duration._

class RLPxTransportProtocolSpec extends FlatSpec with BeforeAndAfterAll {

  val untypedSystem: ActorSystem = untyped.ActorSystem("TypedWatchingUntyped")
  val typedSystem: typed.ActorSystem[_] = untypedSystem.toTyped

  val localPubKey = "ae9025d54592c854fcfdf6a5a9f1e377a124d3492647070e9e6365deef1119e6e046acfd7dd62f6f94d0bc58645e103f78f4c7150933383656ddb6a9fffeb2af"
  val localUri = new URI(s"enode://$localPubKey@0.0.0.0:1234")
  val localAddress = new InetSocketAddress(localUri.getHost, localUri.getPort)

  val remotePubKey = "18a551bee469c2e02de660ab01dede06503c986f6b8520cb5a65ad122df88b17b285e3fef09a40a0d44f99e014f8616cf1ebc2e094f96c6e09e2f390f5d34857"
  val remoteUri = new URI(s"enode://$remotePubKey@47.90.36.129:30303")
  val remoteAddress = new InetSocketAddress(remoteUri.getHost, remoteUri.getPort)

  trait TestFixture {

    val tcpProbe = UntypedTestProbe()(untypedSystem)
    val rlpxConnectionHandler = UntypedTestProbe()(untypedSystem)
    val rLPxConnectionHandlerProps = TestActors.forwardActorProps(rlpxConnectionHandler.ref)

    val rlpxTransportProtocol = new RLPxTransportProtocol[String](
      TestEncoderDecoder.testEncoder, TestEncoderDecoder.testDecoder,
      rLPxConnectionHandlerProps, tcpProbe.ref)

    import rlpxTransportProtocol._

    val transportBehaviour: Behavior[TransportCommand] = rlpxTransportProtocol.createTransport()

    val transportActor: ActorRef[TransportCommand] = untypedSystem.spawnAnonymous(transportBehaviour)
  }

  "RLPx transport protocol" should "open a connection to a valid peer" in new TestFixture {
    import rlpxTransportProtocol._
    val userActor = TestProbe[ConnectionEvent]("userActorProbe")(typedSystem)
    transportActor ! Connect(remoteUri, userActor.ref)

    rlpxConnectionHandler.expectMsg(ConnectTo(remoteUri))
    rlpxConnectionHandler.reply(ConnectionEstablished(ByteString(Hex.decode(remotePubKey))))

    userActor.uponReceivingMessage {
      case Connected(nodeUri, _) =>
        nodeUri shouldBe remoteUri
    }
  }

  it should "close it again" in new TestFixture {
    import rlpxTransportProtocol._
    val userActor = TestProbe[ConnectionEvent]("userActorProbe")(typedSystem)
    transportActor ! Connect(remoteUri, userActor.ref)

    rlpxConnectionHandler.expectMsg(ConnectTo(remoteUri))
    rlpxConnectionHandler.reply(ConnectionEstablished(ByteString(Hex.decode(remotePubKey))))

    userActor.uponReceivingMessage {
      case Connected(nodeId, connection) =>
        nodeId shouldBe remoteUri

        connection ! CloseConnection

        rlpxConnectionHandler.expectMsg(akka.io.Tcp.Close)
        rlpxConnectionHandler.reply(akka.io.Tcp.Closed)

        rlpxConnectionHandler.reply(ConnectionEstablished(ByteString(Hex.decode(remotePubKey))))

        val connectionClosed = userActor.expectMessageType[ConnectionClosed]
        connectionClosed.address shouldBe remoteUri
    }
  }

  it should "report a connection failure to the user" in new TestFixture {
    import rlpxTransportProtocol._
    val userActor = TestProbe[ConnectionEvent]("userActorProbe")(typedSystem)

    transportActor ! Connect(remoteUri, userActor.ref)

    rlpxConnectionHandler.expectMsg(ConnectTo(remoteUri))
    rlpxConnectionHandler.reply(ConnectionFailed)

    val connectionError = userActor.expectMessageType[ConnectionError]
    connectionError.message shouldBe s"Failed to connect to uri $remoteUri"
    connectionError.address shouldBe remoteUri
  }

  it should "enable the creation of inbound connection listeners" in new TestFixture {
    import rlpxTransportProtocol._
    val userActor = TestProbe[ListenerEvent]("userActorProbe")(typedSystem)
    val userConnectionFactory = () => TestProbe[ConnectionEvent]("userActorProbe")(typedSystem).ref

    transportActor ! CreateListener(localUri, userActor.ref, userConnectionFactory)

    tcpProbe.expectMsgClass(classOf[Bind])
    tcpProbe.reply(Bound(new InetSocketAddress(1234)))

    userActor.uponReceivingMessage {
      case Listening(address, _) =>
        address shouldBe localUri
    }
  }

  "Listeners" should "accept incoming connections when listening" in new TestFixture {
    import rlpxTransportProtocol._
    val listenerEventProbe = TestProbe[ListenerEvent]("userActorProbe")(typedSystem)
    val connectionEventProbe = TestProbe[ConnectionEvent]("userActorProbe")(typedSystem)

    val connectionEventHandlerFactory = () => connectionEventProbe.ref

    transportActor ! CreateListener(localUri, listenerEventProbe.ref, connectionEventHandlerFactory)
    tcpProbe.expectMsgType[Bind]
    tcpProbe.reply(Bound(localAddress))
    val listenerBridge = tcpProbe.sender()

    // send it a new connection msg
    listenerBridge ! akka.io.Tcp.Connected(remoteAddress, localAddress)
    rlpxConnectionHandler.expectMsgType[HandleConnection]
    val peerBridge = rlpxConnectionHandler.sender()

    // simulate rlpx handshake success
    peerBridge ! ConnectionEstablished(ByteString(Hex.decode(remotePubKey)))

    connectionEventProbe.uponReceivingMessage {
      case Connected(uri, _) =>
        uri shouldBe remoteUri
    }
  }

  they should "notify users when binding fails" in new TestFixture {
    import rlpxTransportProtocol._
    val userActor = TestProbe[ListenerEvent]("userActorProbe")(typedSystem)
    val userConnectionFactory = () => TestProbe[ConnectionEvent]("userActorProbe")(typedSystem).ref

    transportActor ! CreateListener(localUri, userActor.ref, userConnectionFactory)

    tcpProbe.expectMsgClass(classOf[Bind])
    tcpProbe.reply(CommandFailed(Bind(tcpProbe.ref, localAddress)))

    userActor.expectMessage(ListeningFailed(localUri, s"Error setting up listener on $localUri"))
  }

  ignore should "be unbindable" in new TestFixture {
    import rlpxTransportProtocol._
    val userActor = TestProbe[ListenerEvent]("userActorProbe")(typedSystem)
    val userConnectionFactory = () => TestProbe[ConnectionEvent]("userActorProbe")(typedSystem).ref

    transportActor ! CreateListener(localUri, userActor.ref, userConnectionFactory)

    tcpProbe.expectMsgClass(classOf[Bind])
    tcpProbe.reply(Bound(new InetSocketAddress(1234)))

    userActor.uponReceivingMessage {
      case Listening(_, listenerActor) =>
        listenerActor ! Unbind

        tcpProbe.expectMsg(akka.io.Tcp.Unbind)

        userActor.expectMessage(Unbound(localUri))
    }
  }

  they should "notify users when incoming connections fail" in new TestFixture {
    import rlpxTransportProtocol._
    val listenerEventProbe = TestProbe[ListenerEvent]("userActorProbe")(typedSystem)
    val connectionEventProbe = TestProbe[ConnectionEvent]("userActorProbe")(typedSystem)
    val connectionEventHandlerFactory = () => connectionEventProbe.ref

    transportActor ! CreateListener(localUri, listenerEventProbe.ref, connectionEventHandlerFactory)

    tcpProbe.expectMsgType[Bind]
    tcpProbe.reply(Bound(localAddress))
    val listenerBridge: untyped.ActorRef = tcpProbe.sender()

    listenerBridge ! akka.io.Tcp.Connected(remoteAddress, localAddress)
    rlpxConnectionHandler.expectMsgType[HandleConnection]
    val peerBridge = rlpxConnectionHandler.sender()

    peerBridge ! RLPxConnectionHandler.ConnectionFailed

    connectionEventProbe.uponReceivingMessage {
      case ConnectionError(message, address, _) =>
        address shouldBe new URI(s"enode://@${remoteAddress.getHostName}:${remoteAddress.getPort}")
        message shouldBe s"Error setting up connection with peer at $remoteAddress"
    }
  }

  "Outbound connections" should "support message sending" in new TestFixture {
    import rlpxTransportProtocol._
    val userActor = TestProbe[ConnectionEvent]("userActorProbe")(typedSystem)

    transportActor ! Connect(remoteUri, userActor.ref)

    rlpxConnectionHandler.expectMsg(ConnectTo(remoteUri))
    rlpxConnectionHandler.reply(ConnectionEstablished(ByteString(Hex.decode(remotePubKey))))

    userActor.uponReceivingMessage {
      case Connected(_, connectionActor) =>
        connectionActor ! SendMessage("Hello!")

        rlpxConnectionHandler.expectMsgPF[Assertion](1 second)({
          case io.iohk.cef.network.transport.rlpx.RLPxConnectionHandler.SendMessage(m) =>
            val arr: Array[Byte] = m.toBytes
            new String(arr) shouldBe "Hello!"
        })
    }
  }

  they should "support inbound messages" in new TestFixture {
    import rlpxTransportProtocol._
    val userActor = TestProbe[ConnectionEvent]("userActorProbe")(typedSystem)

    transportActor ! Connect(remoteUri, userActor.ref)

    rlpxConnectionHandler.expectMsg(ConnectTo(remoteUri))
    val connectionBridge = rlpxConnectionHandler.sender()
    rlpxConnectionHandler.reply(ConnectionEstablished(ByteString(Hex.decode(remotePubKey))))

    userActor.uponReceivingMessage {
      case Connected(_, _) =>
        connectionBridge ! RLPxConnectionHandler.MessageReceived(TestMessage("Who are you?"))
        val messageReceived = userActor.expectMessageType[MessageReceived]
        messageReceived.message shouldBe "Who are you?"
        messageReceived.address shouldBe remoteUri
    }
  }

  "Inbound connections" should "support outbound message sending" in new TestFixture {
    import rlpxTransportProtocol._
    val listenerEventProbe = TestProbe[ListenerEvent]("userActorProbe")(typedSystem)
    val connectionEventProbe = TestProbe[ConnectionEvent]("userActorProbe")(typedSystem)
    val connectionEventHandlerFactory = () => connectionEventProbe.ref

    transportActor ! CreateListener(localUri, listenerEventProbe.ref, connectionEventHandlerFactory)

    tcpProbe.expectMsgType[Bind]
    tcpProbe.reply(Bound(localAddress))
    val listenerBridge: untyped.ActorRef = tcpProbe.sender()

    listenerBridge ! akka.io.Tcp.Connected(remoteAddress, localAddress)
    rlpxConnectionHandler.expectMsgType[HandleConnection]
    val peerBridge = rlpxConnectionHandler.sender()

    peerBridge ! ConnectionEstablished(ByteString(Hex.decode(remotePubKey)))

    connectionEventProbe.uponReceivingMessage {
      case Connected(_, connection) =>

        connection ! SendMessage("Hello!")

        rlpxConnectionHandler.expectMsgPF[Assertion](1 second)({
          case io.iohk.cef.network.transport.rlpx.RLPxConnectionHandler.SendMessage(m) =>
            new String(m.toBytes: Array[Byte]) shouldBe "Hello!"
        })
    }
  }

  they should "support inbound messages" in new TestFixture {
    import rlpxTransportProtocol._
    val listenerEventProbe = TestProbe[ListenerEvent]("userActorProbe")(typedSystem)
    val connectionEventProbe = TestProbe[ConnectionEvent]("userActorProbe")(typedSystem)
    val connectionEventHandlerFactory = () => connectionEventProbe.ref

    transportActor ! CreateListener(localUri, listenerEventProbe.ref, connectionEventHandlerFactory)

    tcpProbe.expectMsgType[Bind]
    tcpProbe.reply(Bound(localAddress))
    val listenerBridge: untyped.ActorRef = tcpProbe.sender()

    listenerBridge ! akka.io.Tcp.Connected(remoteAddress, localAddress)
    rlpxConnectionHandler.expectMsgType[HandleConnection]
    val peerBridge = rlpxConnectionHandler.sender()

    peerBridge ! ConnectionEstablished(ByteString(Hex.decode(remotePubKey)))

    connectionEventProbe.uponReceivingMessage {
      case Connected(_, _) =>

        peerBridge ! RLPxConnectionHandler.MessageReceived(TestMessage("Hello!"))

        val messageReceived = connectionEventProbe.expectMessageType[MessageReceived]
        messageReceived.message shouldBe "Hello!"
        messageReceived.address shouldBe remoteUri
    }
  }

  override protected def afterAll(): Unit = {
    typedSystem.terminate()
    untypedSystem.terminate()
  }
}
