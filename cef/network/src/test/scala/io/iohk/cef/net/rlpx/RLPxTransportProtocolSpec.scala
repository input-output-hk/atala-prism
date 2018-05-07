package io.iohk.cef.net.rlpx

import java.net.{InetSocketAddress, URI}

import akka.{actor => untyped}
import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Behavior}
import akka.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter._
import akka.util.ByteString
import io.iohk.cef.net.rlpx.RLPxConnectionHandler.RLPxConfiguration
import io.iohk.cef.net.rlpx.ethereum.p2p.Message.Version
import io.iohk.cef.net.rlpx.ethereum.p2p.MessageDecoder
import io.iohk.cef.net.rlpx.ethereum.p2p.messages.Versions
import io.iohk.cef.net.transport.TransportProtocol.{Connect, Connected, ConnectionReply, TransportMessage}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FunSpec}

import scala.concurrent.duration.FiniteDuration

class RLPxTransportProtocolSpec
    extends FunSpec
    with MockFactory
    with BeforeAndAfterAll {

  val mockMessageDecoder = new MessageDecoder {
    override def fromBytes(`type`: Int,
                           payload: Array[Byte],
                           protocolVersion: Version) =
      throw new Exception("Mock message decoder fails to decode all messages")
  }
  val protocolVersion = Versions.PV63
  val mockHandshaker = mock[AuthHandshaker]
  val mockMessageCodec = mock[MessageCodec]
  val uri = new URI(
    "enode://18a551bee469c2e02de660ab01dede06503c986f6b8520cb5a65ad122df88b17b285e3fef09a40a0d44f99e014f8616cf1ebc2e094f96c6e09e2f390f5d34857@47.90.36.129:30303")
  val inetAddress = new InetSocketAddress(uri.getHost, uri.getPort)
  val rlpxConfiguration = new RLPxConfiguration {
    override val waitForTcpAckTimeout: FiniteDuration = Timeouts.normalTimeout
    override val waitForHandshakeTimeout: FiniteDuration =
      Timeouts.veryLongTimeout
  }

  val rlpxTransportProtocol = new RLPxTransportProtocol(mockMessageDecoder,
                                                        Versions.PV63,
                                                        mockHandshaker,
                                                        rlpxConfiguration)


  val system: ActorSystem = untyped.ActorSystem("TypedWatchingUntyped")

  val transportBehaviour: Behavior[TransportMessage[URI, ByteString]] = rlpxTransportProtocol.createTransport()

  val transportActor: ActorRef[TransportMessage[URI, ByteString]] = system.spawn(transportBehaviour, "Transport")

  val probe: TestProbe[ConnectionReply[ByteString]] = TestProbe[ConnectionReply[ByteString]]()(system.toTyped)


//  val rlpxConnection = untyped.TestActorRef(
//    Props(new RLPxConnectionHandler(mockMessageDecoder, protocolVersion, mockHandshaker, (_, _, _) => mockMessageCodec, rlpxConfiguration) {
//      override def tcpActor: ActorRef = tcpActorProbe.ref
//    }),
//    rlpxConnectionParent.ref)


  describe("RLPx transport protocol") {
    it("should open a connection to a valid peer") {

      transportActor ! Connect(uri, probe.ref)
      probe.expectMessage(Connected(ByteString(
        "18a551bee469c2e02de660ab01dede06503c986f6b8520cb5a65ad122df88b17b285e3fef09a40a0d44f99e014f8616cf1ebc2e094f96c6e09e2f390f5d34857")))

    }

  }
}
