package io.iohk.atala.prism.auth

import java.util.UUID
import io.iohk.atala.prism.kotlin.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import io.iohk.atala.prism.auth
import io.iohk.atala.prism.auth.grpc.SignedRequestsHelper
import io.iohk.atala.prism.kotlin.identity.{PrismDid => DID}
import scalapb.GeneratedMessage

final case class SignedRpcRequest[R <: GeneratedMessage](
    nonce: Vector[Byte],
    signature: ECSignature,
    did: DID,
    keyId: String,
    request: R
)

object SignedRpcRequest {
  def generate[R <: GeneratedMessage](
      keyPair: ECKeyPair,
      did: DID,
      request: R
  ): SignedRpcRequest[R] = {
    val privateKey = keyPair.getPrivateKey
    val requestNonce = UUID.randomUUID().toString.getBytes.toVector
    val signature = EC.signBytes(
      SignedRequestsHelper.merge(auth.model.RequestNonce(requestNonce), request.toByteArray).toArray,
      privateKey
    )
    SignedRpcRequest(
      nonce = requestNonce,
      signature = signature,
      did = did,
      keyId = DID.getMASTER_KEY_ID,
      request = request
    )
  }
}
