package io.iohk.atala.prism.connector

import java.util.Base64

import io.iohk.atala.prism.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}

class RequestAuthenticator {

  /** Signs the connector request, returning the encoded signature and nonce.
    */
  def signConnectorRequest(
      request: Array[Byte],
      privateKey: ECPrivateKey,
      requestNonce: RequestNonce = RequestNonce()
  ): SignedConnectorRequest = {
    val signature = EC.signBytes(requestNonce + request, privateKey)
    SignedConnectorRequest(
      signature = signature.getData,
      requestNonce = requestNonce.bytes
    )
  }
}

case class SignedConnectorRequest(
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
