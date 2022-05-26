package io.iohk.atala.prism.nonce

import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.ECPrivateKey

import java.util.Base64

class RequestAuthenticator {

  /** Signs the connector request, returning the encoded signature and nonce.
    */
  def signConnectorRequest(
      request: Array[Byte],
      privateKey: ECPrivateKey,
      requestNonce: RequestNonce = RequestNonce.random()
  ): SignedNoncedRequest = {
    val signature = EC.signBytes(requestNonce.mergeWith(request).toArray, privateKey)
    SignedNoncedRequest(
      signature = signature.getData,
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
