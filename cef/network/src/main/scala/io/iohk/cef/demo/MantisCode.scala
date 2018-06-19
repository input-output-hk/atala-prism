package io.iohk.cef.demo

import java.net.URI
import java.security.SecureRandom

import akka.actor.Props
import akka.util.ByteString

import io.iohk.cef.network.{ECPublicKeyParametersNodeId, loadAsymmetricCipherKeyPair}
import io.iohk.cef.crypto.keyPairFromPrvKey
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.transport.rlpx.{AuthHandshaker, RLPxConnectionHandler}
import io.iohk.cef.network.transport.rlpx.RLPxConnectionHandler.RLPxConfiguration
import io.iohk.cef.network.transport.rlpx.ethereum.p2p.Message.Version
import io.iohk.cef.network.transport.rlpx.ethereum.p2p.{Message, MessageDecoder, MessageSerializable}

import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.util.encoders.Hex

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

object MantisCode {
  // mantis uses loadAsymmetricCipherKeyPair to lazily generate node keys
  def nodeKeyFromName(nodeName: String): AsymmetricCipherKeyPair = {
    loadAsymmetricCipherKeyPair(s"/tmp/${nodeName}_key", new SecureRandom)
  }

  // how mantis generates node ids.
  private def nodeIdFromKey(nodeKey: AsymmetricCipherKeyPair): Array[Byte] = {
    new ECPublicKeyParametersNodeId(nodeKey.getPublic.asInstanceOf[ECPublicKeyParameters]).toNodeId
  }

  def nodeKeyFromUri(nodeUri: URI): AsymmetricCipherKeyPair = {
    val nodeKeyHex: String = nodeUri.getUserInfo
    val nodeKeyBytes: Array[Byte] = Hex.decode(nodeKeyHex)
    keyPairFromPrvKey(nodeKeyBytes)
  }

  def nodeIdByteStringFromNodeName(nodeName: String): ByteString =
    ByteString(nodeIdFromKey(nodeKeyFromName(nodeName)))


  object MessageConfig {

    case class SampleMessage(content: String) extends MessageSerializable {
      override def toBytes(implicit di: DummyImplicit): ByteString = ByteString(content)

      override def toBytes: Array[Byte] = content.getBytes

      override def underlyingMsg: Message = this

      override def code: Version = 1
    }

    val sampleMessageDecoder = new MessageDecoder {
      override def fromBytes(`type`: Int, payload: Array[Byte], protocolVersion: Version): Message = SampleMessage(new String(payload))
    }

    val sampleEncoder: Encoder[String, ByteString] = ByteString(_)

    val sampleDecoder: Decoder[Message, String] = {
      case SampleMessage(content) => content
      case _ => throw new UnsupportedOperationException(s"This is a dummy test decoder and it only supports ${classOf[SampleMessage]}")
    }
  }

  def rlpxProps(nodeKey: AsymmetricCipherKeyPair): Props = {

    val rlpxConfiguration = new RLPxConfiguration {
      override val waitForHandshakeTimeout: FiniteDuration = 30 seconds
      override val waitForTcpAckTimeout: FiniteDuration = 30 seconds
    }

    RLPxConnectionHandler.props(
      MessageConfig.sampleMessageDecoder, protocolVersion = 1, AuthHandshaker(nodeKey, new SecureRandom()), rlpxConfiguration)
  }

}
