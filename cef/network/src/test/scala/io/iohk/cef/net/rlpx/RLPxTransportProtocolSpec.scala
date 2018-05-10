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
import io.iohk.cef.net.transport.TransportProtocol._
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.concurrent.duration._

class RLPxTransportProtocolSpec extends FlatSpec with BeforeAndAfterAll {

  val localPubKey = "ae9025d54592c854fcfdf6a5a9f1e377a124d3492647070e9e6365deef1119e6e046acfd7dd62f6f94d0bc58645e103f78f4c7150933383656ddb6a9fffeb2af"
  val localUri = new URI(s"enode://$localPubKey@0.0.0.0:1234")

  val remotePubKey = "18a551bee469c2e02de660ab01dede06503c986f6b8520cb5a65ad122df88b17b285e3fef09a40a0d44f99e014f8616cf1ebc2e094f96c6e09e2f390f5d34857"
  val remoteUri = new URI(s"enode://$remotePubKey@47.90.36.129:30303")

  val untypedSystem: ActorSystem = untyped.ActorSystem("TypedWatchingUntyped")
  val typedSystem: typed.ActorSystem[_] = untypedSystem.toTyped

  val tcpProbe = UntypedTestProbe()(untypedSystem)
  val rlpxConnectionHandler = UntypedTestProbe()(untypedSystem)
  val rLPxConnectionHandlerProps = () => TestActors.forwardActorProps(rlpxConnectionHandler.ref)
  val rlpxTransportProtocol = new RLPxTransportProtocol(rLPxConnectionHandlerProps, tcpProbe.ref)

  val transportBehaviour: Behavior[TransportCommand[URI, ByteString]] = rlpxTransportProtocol.createTransport()

  val transportActor: ActorRef[TransportCommand[URI, ByteString]] = untypedSystem.spawn(transportBehaviour, "Transport")

  "RLPx transport protocol" should "open a connection to a valid peer" in {
    val userActor = TestProbe[ConnectionReply[ByteString]]("userActorProbe")(typedSystem)

    transportActor ! Connect(remoteUri, userActor.ref)

    rlpxConnectionHandler.expectMsg(ConnectTo(remoteUri))
    rlpxConnectionHandler.reply(ConnectionEstablished(ByteString(remotePubKey)))

    userActor.expectMessage(Connected(ByteString(remotePubKey)))
  }

  it should "report a connection failure to the user" in {
    val userActor = TestProbe[ConnectionReply[ByteString]]("userActorProbe")(typedSystem)

    transportActor ! Connect(remoteUri, userActor.ref)

    rlpxConnectionHandler.expectMsg(ConnectTo(remoteUri))
    rlpxConnectionHandler.reply(ConnectionFailed)

    userActor.expectMessage(ConnectionError(s"Failed to connect to uri $remoteUri", ByteString(remotePubKey)))
  }

  it should "enable the creation of inbound connection listeners" in {
    val userActor = TestProbe[ListenerCreated[URI, ByteString]]("userActorProbe")(typedSystem)

    transportActor ! CreateListener(userActor.ref)

    userActor.expectMessageType[ListenerCreated[URI, ByteString]]
  }

  "Inbound connection listeners" should "listen on a given address" in {
    val userActor = TestProbe[ListenerEvent[URI, ByteString]]("userActorProbe")(typedSystem)
    val listener: ActorRef[ListenerCommand[URI, ByteString]] = createListener(typedSystem)

    listener ! Listen(localUri, userActor.ref)

    tcpProbe.expectMsgClass(classOf[Bind])
    tcpProbe.reply(Bound(new InetSocketAddress(1234)))

    userActor.expectMessage(1 second, Listening[URI, ByteString](localUri))
  }

  they should "accept incoming connections when listening" in {
    val userActor = TestProbe[ListenerEvent[URI, ByteString]]("userActorProbe")(typedSystem)

    createListener(typedSystem) ! Listen(localUri, userActor.ref)

    val bindMsg = tcpProbe.expectMsgType[Bind]
    val bindHandler: untyped.ActorRef = bindMsg.handler
    bindHandler ! akka.io.Tcp.Connected(new InetSocketAddress(remoteUri.getHost, remoteUri.getPort), new InetSocketAddress(localUri.getHost, localUri.getPort))
    rlpxConnectionHandler.expectMsgType[HandleConnection]

    bindHandler ! ConnectionEstablished(ByteString(remotePubKey))

    userActor.expectMessage(1 second, ConnectionReceived[URI, ByteString](ByteString(remotePubKey)))
  }

  private def createListener(actorSystem: typed.ActorSystem[_]): ActorRef[ListenerCommand[URI, ByteString]] = {
    val tp = TestProbe[ListenerCreated[URI, ByteString]]("listener-created-probe")(actorSystem)

    transportActor ! CreateListener(tp.ref)

    tp.expectMessageType[ListenerCreated[URI, ByteString]].listener
  }

  override protected def afterAll(): Unit = {
    typedSystem.terminate()
    untypedSystem.terminate()
  }
}
