package io.iohk.cef.network.transport.rlpx.ethereum.p2p.messages

import akka.util.ByteString
import io.iohk.cef.network.transport.rlpx.ethereum.p2p.{Message, MessageSerializableImplicit}
import io.iohk.cef.network.transport.rlpx.ethereum.p2p.messages.WireProtocol.Disconnect.Reasons
import io.iohk.cef.encoding.rlp.RLPImplicits._
import io.iohk.cef.encoding.rlp._
import io.iohk.cef.utils.HexStringCodec._

object WireProtocol {

  private val sEncDec = implicitly[RLPEncDec[String]]
  private val bEncDec = implicitly[RLPEncDec[Byte]]
  private val lEncDec = implicitly[RLPEncDec[Long]]
  private val bsEncDec = implicitly[RLPEncDec[ByteString]]

  object Capability {
    implicit class CapabilityEnc(val msg: Capability) extends RLPSerializable {
      override def toRLPEncodable: RLPEncodeable = RLPList(sEncDec.encode(msg.name), bEncDec.encode(msg.version))
    }

    implicit class CapabilityDec(val bytes: Array[Byte]) extends AnyVal {
      def toCapability: Capability = CapabilityRLPEncodableDec(rawDecode(bytes)).toCapability
    }

    implicit class CapabilityRLPEncodableDec(val rLPEncodeable: RLPEncodeable) extends AnyVal {
      def toCapability: Capability = rLPEncodeable match {
        case RLPList(name, version) => Capability(sEncDec.decode(name), bEncDec.decode(version))
        case _ => throw new RuntimeException("Cannot decode Capability")
      }
    }
  }

  case class Capability(name: String, version: Byte)

  object Hello {

    val code = 0x00

    implicit class HelloEnc(val underlyingMsg: Hello)
        extends MessageSerializableImplicit[Hello](underlyingMsg)
        with RLPSerializable {

      override def code: Int = Hello.code

      override def toRLPEncodable: RLPEncodeable = {
        import msg._
        RLPList(
          lEncDec.encode(p2pVersion),
          sEncDec.encode(clientId),
          RLPList(capabilities.map(_.toRLPEncodable): _*),
          lEncDec.encode(listenPort),
          bsEncDec.encode(nodeId))
      }
    }

    implicit class HelloDec(val bytes: Array[Byte]) extends AnyVal {
      import Capability._

      def toHello: Hello = rawDecode(bytes) match {
        case RLPList(p2pVersion, clientId, (capabilities: RLPList), listenPort, nodeId, _*) =>
          Hello(
            lEncDec.decode(p2pVersion),
            sEncDec.decode(clientId),
            capabilities.items.map(_.toCapability),
            lEncDec.decode(listenPort),
            bsEncDec.decode(nodeId))
        case _ => throw new RuntimeException("Cannot decode Hello")
      }
    }
  }

  case class Hello(
      p2pVersion: Long,
      clientId: String,
      capabilities: Seq[Capability],
      listenPort: Long,
      nodeId: ByteString)
      extends Message {

    override val code: Int = Hello.code

    override def toString: String = {
      s"""Hello {
         |p2pVersion: $p2pVersion
         |clientId: $clientId
         |capabilities: $capabilities
         |listenPort: $listenPort
         |nodeId: ${toHexString(nodeId)}
         |}""".stripMargin
    }
  }

  object Disconnect {
    object Reasons {
      val DisconnectRequested = 0x00
      val TcpSubsystemError = 0x01
      val UselessPeer = 0x03
      val TooManyPeers = 0x04
      val AlreadyConnected = 0x05
      val IncompatibleP2pProtocolVersion = 0x06
      val NullNodeIdentityReceived = 0x07
      val ClientQuitting = 0x08
      val UnexpectedIdentity = 0x09
      val IdentityTheSame = 0xa
      val TimeoutOnReceivingAMessage = 0x0b
      val Other = 0x10
    }

    val code = 0x01

    implicit class DisconnectEnc(val underlyingMsg: Disconnect)
        extends MessageSerializableImplicit[Disconnect](underlyingMsg)
        with RLPSerializable {
      override def code: Int = Disconnect.code

      override def toRLPEncodable: RLPEncodeable = RLPList(lEncDec.encode(msg.reason))
    }

    implicit class DisconnectDec(val bytes: Array[Byte]) extends AnyVal {
      def toDisconnect: Disconnect = rawDecode(bytes) match {
        case RLPList(reason, _*) => Disconnect(reason = lEncDec.decode(reason))
        case _ => throw new RuntimeException("Cannot decode Disconnect")
      }
    }
  }

  case class Disconnect(reason: Long) extends Message {
    override val code: Int = Disconnect.code

    override def toString: String = {

      val message = reason match {
        case Reasons.DisconnectRequested => "Disconnect requested"
        case Reasons.TcpSubsystemError => "TCP sub-system error"
        case Reasons.UselessPeer => "Useless peer"
        case Reasons.TooManyPeers => "Too many peers"
        case Reasons.AlreadyConnected => "Already connected"
        case Reasons.IncompatibleP2pProtocolVersion => "Incompatible P2P protocol version"
        case Reasons.NullNodeIdentityReceived => "Null node identity received - this is automatically invalid"
        case Reasons.ClientQuitting => "Client quitting"
        case Reasons.UnexpectedIdentity => "Unexpected identity"
        case Reasons.IdentityTheSame => "Identity is the same as this node"
        case Reasons.TimeoutOnReceivingAMessage => "Timeout on receiving a message"
        case Reasons.Other => "Some other reason specific to a subprotocol"
        case other => s"unknown reason code: $other"
      }

      s"Disconnect($message)"
    }
  }

  object Ping {

    val code = 0x02

    implicit class PingEnc(val underlyingMsg: Ping)
        extends MessageSerializableImplicit[Ping](underlyingMsg)
        with RLPSerializable {
      override def code: Int = Ping.code

      override def toRLPEncodable: RLPEncodeable = RLPList()
    }

    implicit class PingDec(val bytes: Array[Byte]) extends AnyVal {
      def toPing: Ping = Ping()
    }
  }

  case class Ping() extends Message {
    override val code: Int = Ping.code
  }

  object Pong {

    val code = 0x03

    implicit class PongEnc(val underlyingMsg: Pong)
        extends MessageSerializableImplicit[Pong](underlyingMsg)
        with RLPSerializable {
      override def code: Int = Pong.code

      override def toRLPEncodable: RLPEncodeable = RLPList()
    }

    implicit class PongDec(val bytes: Array[Byte]) extends AnyVal {
      def toPong: Pong = Pong()
    }
  }

  case class Pong() extends Message {
    override val code: Int = Pong.code
  }

}
