package io.iohk.atala.requests

import java.util.Base64

import io.iohk.atala.crypto.{ECPrivateKey, ECSignature, ECTrait}

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
    SignedConnectorRequest(encodedSignature = encode(signature), encodedRequestNonce = encode(requestNonce))
  }

  private def encode(signature: ECSignature): String = {
    Base64.getUrlEncoder.encodeToString(signature.data)
  }

  private def encode(requestNonce: RequestNonce): String = {
    Base64.getUrlEncoder.encodeToString(requestNonce.bytes)
  }
}

case class SignedConnectorRequest(encodedSignature: String, encodedRequestNonce: String)
