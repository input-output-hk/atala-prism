package io.iohk.atala.prism.node.nonce

import io.iohk.atala.prism.node.auth.model.RequestNonce
import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpECDSA, SecpPrivateKey}

import java.util.Base64

class RequestAuthenticator {

  def signConnectorRequest(
      request: Array[Byte],
      privateKey: SecpPrivateKey,
      requestNonce: RequestNonce = RequestNonce.random()
  ): SignedNoncedRequest = {
    val signature = SecpECDSA.signBytes(requestNonce.mergeWith(request).toArray, privateKey)
    SignedNoncedRequest(
      signature = signature.bytes,
      requestNonce = requestNonce.bytes.toArray
    )
  }
}

case class SignedNoncedRequest(
    signature: Array[Byte],
    requestNonce: Array[Byte]
) {
  def encodedSignature: String = {
    encode(signature)
  }

  def encodedRequestNonce: String = {
    encode(requestNonce)
  }

  private def encode(bytes: Array[Byte]): String = {
    Base64.getUrlEncoder.encodeToString(bytes)
  }
}
