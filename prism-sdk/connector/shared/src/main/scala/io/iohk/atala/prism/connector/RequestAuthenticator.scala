package io.iohk.atala.prism.connector

import java.util.Base64

import io.iohk.atala.prism.crypto.{ECPrivateKey, ECTrait}

class RequestAuthenticator(ec: ECTrait) {

  /**
    * Signs the connector request, returning the encoded signature and nonce.
    */
  def signConnectorRequest(
      request: Array[Byte],
      privateKey: ECPrivateKey
  ): SignedConnectorRequest = {
    val requestNonce = RequestNonce()
    val signature = ec.sign(requestNonce + request, privateKey)
    SignedConnectorRequest(signature = signature.data, requestNonce = requestNonce.bytes)
  }
}

case class SignedConnectorRequest(signature: Array[Byte], requestNonce: Array[Byte]) {
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
