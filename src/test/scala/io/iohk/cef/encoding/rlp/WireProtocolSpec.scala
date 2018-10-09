package io.iohk.cef.encoding.rlp

import akka.util.ByteString
import io.iohk.cef.network.transport.rlpx.ethereum.p2p.messages.WireProtocol
import io.iohk.cef.network.transport.rlpx.ethereum.p2p.messages.WireProtocol._
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

class WireProtocolSpec extends FlatSpec with MustMatchers with PropertyChecks {

  val capabilityGen = for {
    str <- Gen.alphaNumStr
    byte <- Gen.chooseNum(0, 127).map(_.toByte)
  } yield Capability(str, byte)

  val helloMsgGen = for {
    version <- Gen.chooseNum(0, Long.MaxValue)
    clientId <- Gen.alphaNumStr
    capabilities <- Gen.listOf(capabilityGen)
    port <- Gen.chooseNum(0, Long.MaxValue)
    nodeId <- Gen.listOf(Gen.chooseNum(0, 127).map(_.toByte)).map(l => ByteString(l.toArray))
  } yield Hello(version, clientId, capabilities, port, nodeId)

  behavior of "WireProtocol"

  it should "encode an decode capabilities" in {
    import WireProtocol.Capability._
    forAll(capabilityGen) { (c: Capability) =>
      c.toRLPEncodable.toCapability mustBe c
      RLP.encode(c.toRLPEncodable).toCapability mustBe c
    }
  }
  it should "encode and decode hello msg" in {
    forAll(helloMsgGen) { (h: Hello) =>
      import WireProtocol.Hello._
      RLP.encode(h.toRLPEncodable).toHello mustBe h
    }
  }
  it should "encode and decode a disconnect" in {
    import io.iohk.cef.network.transport.rlpx.ethereum.p2p.messages.WireProtocol.Disconnect._
    val reasons = Seq(
      Reasons.DisconnectRequested,
      Reasons.TcpSubsystemError,
      Reasons.UselessPeer,
      Reasons.TooManyPeers,
      Reasons.AlreadyConnected,
      Reasons.IncompatibleP2pProtocolVersion,
      Reasons.NullNodeIdentityReceived,
      Reasons.ClientQuitting,
      Reasons.UnexpectedIdentity,
      Reasons.IdentityTheSame,
      Reasons.TimeoutOnReceivingAMessage,
      Reasons.Other
    ).map(r => Disconnect(r))
    reasons.foreach(d => RLP.encode(d.toRLPEncodable).toDisconnect mustBe d)
  }
  it should "encode and decode a Ping" in {
    import io.iohk.cef.network.transport.rlpx.ethereum.p2p.messages.WireProtocol.Ping._
    RLP.encode(Ping().toRLPEncodable).toPing mustBe Ping()
  }
  it should "encode and decode a Pong" in {
    import io.iohk.cef.network.transport.rlpx.ethereum.p2p.messages.WireProtocol.Pong._
    RLP.encode(Pong().toRLPEncodable).toPong mustBe Pong()
  }

  val validDisconnectCodes = Table(
    ("Code", "Reason"),
    (0x00, "Disconnect requested"),
    (0x01, "TCP sub-system error"),
    (0x03, "Useless peer"),
    (0x04, "Too many peers"),
    (0x05, "Already connected"),
    (0x06, "Incompatible P2P protocol version"),
    (0x07, "Null node identity received - this is automatically invalid"),
    (0x08, "Client quitting"),
    (0x09, "Unexpected identity"),
    (0xa, "Identity is the same as this node"),
    (0x0b, "Timeout on receiving a message"),
    (0x10, "Some other reason specific to a subprotocol")
  )

  forAll(validDisconnectCodes) { (code, reason) =>
    it should f"print a human readable reason for code $code%h" in {
      Disconnect(code).toString mustBe s"Disconnect($reason)"
    }
  }

  it should "print a readable reason for an invalid code" in {
    Disconnect(101).toString mustBe s"Disconnect(unknown reason code: 101)"
  }
}
