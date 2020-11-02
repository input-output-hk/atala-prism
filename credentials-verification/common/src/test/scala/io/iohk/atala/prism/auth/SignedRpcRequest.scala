package io.iohk.atala.prism.auth

import java.util.UUID

import io.iohk.atala.prism.crypto.{EC, ECKeyPair, ECSignature}
import io.iohk.atala.prism.auth
import io.iohk.atala.prism.auth.grpc.SignedRequestsHelper
import scalapb.GeneratedMessage

final case class SignedRpcRequest[R <: GeneratedMessage](
    nonce: Vector[Byte],
    signature: ECSignature,
    did: String,
    keyId: String,
    request: R
)

object SignedRpcRequest {
  def generate[R <: GeneratedMessage](
      keyPair: ECKeyPair,
      did: String,
      request: R
  ): SignedRpcRequest[R] = {
    val privateKey = keyPair.privateKey
    val requestNonce = UUID.randomUUID().toString.getBytes.toVector
    val signature = EC.sign(
      SignedRequestsHelper.merge(auth.model.RequestNonce(requestNonce), request.toByteArray).toArray,
      privateKey
    )
    SignedRpcRequest(
      nonce = requestNonce,
      signature = signature,
      did = did,
      keyId = "master0",
      request = request
    )
  }
}
