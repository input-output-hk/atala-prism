package io.iohk.cef.net.rlpx

import java.net.{InetSocketAddress, URI}

import akka.actor.{ActorSystem, typed}
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, Behavior}
import akka.io.Tcp.{Bind, Bound}
import akka.testkit.typed.scaladsl.TestProbe
import akka.testkit.{TestActors, TestProbe => UntypedTestProbe}
import akka.util.ByteString
import akka.{actor => untyped}
import io.iohk.cef.net.rlpx.RLPxConnectionHandler.{ConnectTo, ConnectionEstablished, ConnectionFailed, HandleConnection}
import io.iohk.cef.test.TestEncoderDecoder
import io.iohk.cef.test.TestEncoderDecoder.TestMessage
import io.iohk.cef.test.TypedTestProbeOps._
import org.scalatest.{Assertion, BeforeAndAfterAll, FlatSpec}
import org.scalatest.Matchers._

import scala.concurrent.duration._

class RLPxTransportProtocolSpec extends FlatSpec with BeforeAndAfterAll {

  val localPubKey = "ae9025d54592c854fcfdf6a5a9f1e377a124d3492647070e9e6365deef1119e6e046acfd7dd62f6f94d0bc58645e103f78f4c7150933383656ddb6a9fffeb2af"
  val localUri = new URI(s"enode://$localPubKey@0.0.0.0:1234")
  val localAddress = new InetSocketAddress(localUri.getHost, localUri.getPort)

  val remotePubKey = "18a551bee469c2e02de660ab01dede06503c986f6b8520cb5a65ad122df88b17b285e3fef09a40a0d44f99e014f8616cf1ebc2e094f96c6e09e2f390f5d34857"
  val remoteUri = new URI(s"enode://$remotePubKey@47.90.36.129:30303")
  val remoteAddress = new InetSocketAddress(remoteUri.getHost, remoteUri.getPort)

  val untypedSystem: ActorSystem = untyped.ActorSystem("TypedWatchingUntyped")
  val typedSystem: typed.ActorSystem[_] = untypedSystem.toTyped

  val tcpProbe = UntypedTestProbe()(untypedSystem)
  val rlpxConnectionHandler = UntypedTestProbe()(untypedSystem)
  val rLPxConnectionHandlerProps = () => TestActors.forwardActorProps(rlpxConnectionHandler.ref)

  val rlpxTransportProtocol = new RLPxTransportProtocol[String](
    TestEncoderDecoder.testEncoder, TestEncoderDecoder.testDecoder,
    rLPxConnectionHandlerProps, tcpProbe.ref)

  import rlpxTransportProtocol._

  val transportBehaviour: Behavior[TransportCommand] = rlpxTransportProtocol.createTransport()

  val transportActor: ActorRef[TransportCommand] = untypedSystem.spawn(transportBehaviour, "Transport")


  "RLPx transport protocol" should "open a connection to a valid peer" in {
    val userActor = TestProbe[ConnectionEvent]("userActorProbe")(typedSystem)

    transportActor ! Connect(remoteUri, userActor.ref)

    rlpxConnectionHandler.expectMsg(ConnectTo(remoteUri))
    rlpxConnectionHandler.reply(ConnectionEstablished(ByteString(remotePubKey)))

    userActor.uponReceivingMessage {
      case Connected(nodeId, _) =>
        nodeId shouldBe remoteUri
    }
  }

  it should "report a connection failure to the user" in {
    val userActor = TestProbe[ConnectionEvent]("userActorProbe")(typedSystem)
    transportActor ! Connect(remoteUri, userActor.ref)

    rlpxConnectionHandler.expectMsg(ConnectTo(remoteUri))
    rlpxConnectionHandler.reply(ConnectionFailed)

    userActor.expectMessage(ConnectionError(s"Failed to connect to uri $remoteUri", remoteUri))
  }

  it should "enable the creation of inbound connection listeners" in {
    val userActor = TestProbe[ListenerEvent]("userActorProbe")(typedSystem)
    val userConnectionFactory = (_: URI) => TestProbe[ConnectionEvent]("userActorProbe")(typedSystem).ref

    transportActor ! CreateListener(localUri, userActor.ref, userConnectionFactory)

    tcpProbe.expectMsgClass(classOf[Bind])
    tcpProbe.reply(Bound(new InetSocketAddress(1234)))

    userActor.expectMessage(1 second, Listening(localUri))
  }

  "Listeners" should "accept incoming connections when listening" in {
    val listenerEventProbe = TestProbe[ListenerEvent]("userActorProbe")(typedSystem)
    val connectionEventProbe = TestProbe[ConnectionEvent]("userActorProbe")(typedSystem)

    val connectionEventHandlerFactory = (_: URI) => connectionEventProbe.ref

    transportActor ! CreateListener(localUri, listenerEventProbe.ref, connectionEventHandlerFactory)
    tcpProbe.expectMsgType[Bind]
    val listenerBridge = tcpProbe.sender()

    // send it a new connection msg
    listenerBridge ! akka.io.Tcp.Connected(remoteAddress, localAddress)
    rlpxConnectionHandler.expectMsgType[HandleConnection]
    val peerBridge = rlpxConnectionHandler.sender()

    // simulate rlpx handshake success
    peerBridge ! ConnectionEstablished(ByteString(remotePubKey))

    connectionEventProbe.uponReceivingMessage {
      case Connected(uri, _) =>
        uri shouldBe remoteUri
    }
  }

  they should "notify users when binding fails" in pending
  they should "notify users when incoming connections fail" in pending

  "Outbound connections" should "support message sending" in {
    val userActor = TestProbe[ConnectionEvent]("userActorProbe")(typedSystem)
    transportActor ! Connect(remoteUri, userActor.ref)

    rlpxConnectionHandler.expectMsg(ConnectTo(remoteUri))
    rlpxConnectionHandler.reply(ConnectionEstablished(ByteString(remotePubKey)))

    userActor.uponReceivingMessage {
      case Connected(_, connectionActor) =>
        connectionActor ! SendMessage("Hello!")

        rlpxConnectionHandler.expectMsgPF[Assertion](1 second)({
          case io.iohk.cef.net.rlpx.RLPxConnectionHandler.SendMessage(m) =>
            val arr: Array[Byte] = m.toBytes
            new String(arr) shouldBe "Hello!"
        })
    }
  }

  they should "support inbound messages" in {
    val userActor = TestProbe[ConnectionEvent]("userActorProbe")(typedSystem)
    transportActor ! Connect(remoteUri, userActor.ref)

    rlpxConnectionHandler.expectMsg(ConnectTo(remoteUri))
    val connectionBridge = rlpxConnectionHandler.sender()
    rlpxConnectionHandler.reply(ConnectionEstablished(ByteString(remotePubKey)))

    userActor.uponReceivingMessage {
      case Connected(_, _) =>
        connectionBridge ! RLPxConnectionHandler.MessageReceived(TestMessage("Who are you?"))
        userActor.expectMessage(MessageReceived("Who are you?"))
    }
  }

  "Inbound connections" should "support outbound message sending" in {
    val listenerEventProbe = TestProbe[ListenerEvent]("userActorProbe")(typedSystem)
    val connectionEventProbe = TestProbe[ConnectionEvent]("userActorProbe")(typedSystem)
    val connectionEventHandlerFactory = (_: URI) => connectionEventProbe.ref

    transportActor ! CreateListener(localUri, listenerEventProbe.ref, connectionEventHandlerFactory)

    tcpProbe.expectMsgType[Bind]
    val listenerBridge: untyped.ActorRef = tcpProbe.sender()

    listenerBridge ! akka.io.Tcp.Connected(remoteAddress, localAddress)
    rlpxConnectionHandler.expectMsgType[HandleConnection]
    val peerBridge = rlpxConnectionHandler.sender()

    peerBridge ! ConnectionEstablished(ByteString(remotePubKey))

    connectionEventProbe.uponReceivingMessage {
      case Connected(_, connection) =>

        connection ! SendMessage("Hello!")

        rlpxConnectionHandler.expectMsgPF[Assertion](1 second)({
          case io.iohk.cef.net.rlpx.RLPxConnectionHandler.SendMessage(m) =>
            new String(m.toBytes: Array[Byte]) shouldBe "Hello!"
        })
    }
  }

  they should "support inbound messages" in {
    val listenerEventProbe = TestProbe[ListenerEvent]("userActorProbe")(typedSystem)
    val connectionEventProbe = TestProbe[ConnectionEvent]("userActorProbe")(typedSystem)
    val connectionEventHandlerFactory = (_: URI) => connectionEventProbe.ref

    transportActor ! CreateListener(localUri, listenerEventProbe.ref, connectionEventHandlerFactory)

    tcpProbe.expectMsgType[Bind]
    val listenerBridge: untyped.ActorRef = tcpProbe.sender()

    listenerBridge ! akka.io.Tcp.Connected(remoteAddress, localAddress)
    rlpxConnectionHandler.expectMsgType[HandleConnection]
    val peerBridge = rlpxConnectionHandler.sender()

    peerBridge ! ConnectionEstablished(ByteString(remotePubKey))

    connectionEventProbe.uponReceivingMessage {
      case Connected(_, _) =>

        peerBridge ! RLPxConnectionHandler.MessageReceived(TestMessage("Hello!"))

        connectionEventProbe.expectMessage(MessageReceived("Hello!"))
    }
  }

  override protected def afterAll(): Unit = {
    typedSystem.terminate()
    untypedSystem.terminate()
  }
}
