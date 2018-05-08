package io.iohk.cef.net.rlpx

import java.net.URI

import akka.actor.{ActorSystem, typed}
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, Behavior}
import akka.testkit.typed.scaladsl.TestProbe
import akka.testkit.{TestActors, TestProbe => UntypedTestProbe}
import akka.util.ByteString
import akka.{actor => untyped}

import io.iohk.cef.net.rlpx.RLPxConnectionHandler.{ConnectTo, ConnectionEstablished, ConnectionFailed}
import io.iohk.cef.net.transport.TransportProtocol._

import org.scalatest.{BeforeAndAfterAll, FlatSpec}

class RLPxTransportProtocolSpec extends FlatSpec with BeforeAndAfterAll {

  val remotePubKey = "18a551bee469c2e02de660ab01dede06503c986f6b8520cb5a65ad122df88b17b285e3fef09a40a0d44f99e014f8616cf1ebc2e094f96c6e09e2f390f5d34857"
  val uri = new URI(s"enode://$remotePubKey@47.90.36.129:30303")

  val untypedSystem: ActorSystem = untyped.ActorSystem("TypedWatchingUntyped")
  val typedSystem: typed.ActorSystem[_] = untypedSystem.toTyped

  val rlpxConnectionHandler = UntypedTestProbe()(untypedSystem)
  val rLPxConnectionHandlerProps = () => TestActors.forwardActorProps(rlpxConnectionHandler.ref)
  val rlpxTransportProtocol = new RLPxTransportProtocol(rLPxConnectionHandlerProps)

  val transportBehaviour: Behavior[TransportCommand[URI, ByteString]] = rlpxTransportProtocol.createTransport()

  val transportActor: ActorRef[TransportCommand[URI, ByteString]] = untypedSystem.spawn(transportBehaviour, "Transport")

  val userActor: TestProbe[ConnectionReply[ByteString]] = TestProbe[ConnectionReply[ByteString]]("userActorProbe")(typedSystem)


  "RLPx transport protocol" should "open a connection to a valid peer" in {

    transportActor ! Connect(uri, userActor.ref)

    rlpxConnectionHandler.expectMsg(ConnectTo(uri))
    rlpxConnectionHandler.reply(ConnectionEstablished(ByteString(remotePubKey)))

    userActor.expectMessage(Connected(ByteString(remotePubKey)))
  }

  it should "report a connection failure to the user" in {

    transportActor ! Connect(uri, userActor.ref)

    rlpxConnectionHandler.expectMsg(ConnectTo(uri))
    rlpxConnectionHandler.reply(ConnectionFailed)

    userActor.expectMessage(ConnectionError(s"Failed to connect to uri $uri", ByteString(remotePubKey)))
  }


  override protected def afterAll(): Unit = {
    typedSystem.terminate()
    untypedSystem.terminate()
  }
}
