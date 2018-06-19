package io.iohk.cef.demo

import java.security.SecureRandom

import akka.actor.Props
import akka.util.ByteString

import io.iohk.cef.network.{ECPublicKeyParametersNodeId, loadAsymmetricCipherKeyPair}
import io.iohk.cef.network.transport.rlpx.{AuthHandshaker, RLPxConnectionHandler}
import io.iohk.cef.network.transport.rlpx.RLPxConnectionHandler.RLPxConfiguration

import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.ECPublicKeyParameters

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

object MantisCode {
  // mantis uses loadAsymmetricCipherKeyPair to lazily generate node keys
  def nodeKeyFromName(nodeName: String): AsymmetricCipherKeyPair = {
    loadAsymmetricCipherKeyPair(s"/tmp/${nodeName}_key", new SecureRandom)
  }

  // how mantis generates node ids.
  def nodeIdFromKey(nodeKey: AsymmetricCipherKeyPair): Array[Byte] = {
    new ECPublicKeyParametersNodeId(nodeKey.getPublic.asInstanceOf[ECPublicKeyParameters]).toNodeId
  }

  def nodeIdByteStringFromNodeName(nodeName: String): ByteString =
    ByteString(nodeIdFromKey(nodeKeyFromName(nodeName)))


  def rlpxProps(nodeKey: AsymmetricCipherKeyPair): Props = {

    val rlpxConfiguration = new RLPxConfiguration {
      override val waitForHandshakeTimeout: FiniteDuration = 30 seconds
      override val waitForTcpAckTimeout: FiniteDuration = 30 seconds
    }

    RLPxConnectionHandler.props(
      MessageConfig.sampleMessageDecoder, protocolVersion = 1, AuthHandshaker(nodeKey, new SecureRandom()), rlpxConfiguration)
  }
}
