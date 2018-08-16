package io.iohk.cef.builder

import java.net.URI

import akka.util.ByteString
import io.iohk.cef.cryptolegacy
import io.iohk.cef.cryptolegacy._
import io.iohk.cef.network.transport.rlpx.{AuthHandshakeSuccess, AuthHandshaker, Secrets}
import AuthHandshaker._
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.util.encoders.Hex

trait SecureChannelSetup extends SecureRandomBuilder {

  val remoteNodeKey: AsymmetricCipherKeyPair = generateKeyPair(secureRandom)
  val remoteEphemeralKey: AsymmetricCipherKeyPair = generateKeyPair(secureRandom)
  val remoteNonce: ByteString = randomNonce()
  val remoteNodeId: Array[Byte] = remoteNodeKey.getPublic.asInstanceOf[ECPublicKeyParameters].toNodeId
  val remoteUri = new URI(s"enode://${Hex.toHexString(remoteNodeId)}@127.0.0.1:30303")

  val nodeKey: AsymmetricCipherKeyPair = generateKeyPair(secureRandom)
  val ephemeralKey: AsymmetricCipherKeyPair = generateKeyPair(secureRandom)
  val nonce: ByteString = randomNonce()

  val handshaker = AuthHandshaker(nodeKey, nonce, ephemeralKey, secureRandom)
  val remoteHandshaker = AuthHandshaker(remoteNodeKey, remoteNonce, remoteEphemeralKey, secureRandom)

  val (initPacket, handshakerInitiated) = handshaker.initiate(remoteUri)
  val (responsePacket, AuthHandshakeSuccess(remoteSecrets: Secrets, _)) = remoteHandshaker.handleInitialMessageV4(initPacket)
  val AuthHandshakeSuccess(secrets: Secrets, _) = handshakerInitiated.handleResponseMessageV4(responsePacket)

  def randomNonce(): ByteString = cryptolegacy.secureRandomByteString(secureRandom, AuthHandshaker.NonceSize)
}
