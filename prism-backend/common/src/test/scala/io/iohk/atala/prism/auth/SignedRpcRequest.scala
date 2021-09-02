package io.iohk.atala.prism.auth

import java.util.UUID
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import io.iohk.atala.prism.auth
import io.iohk.atala.prism.auth.grpc.SignedRequestsHelper
import io.iohk.atala.prism.kotlin.identity.PrismDid
import scalapb.GeneratedMessage

final case class SignedRpcRequest[R <: GeneratedMessage](
    nonce: Vector[Byte],
    signature: ECSignature,
    did: PrismDid,
    keyId: String,
    request: R
)

object SignedRpcRequest {
  def generate[R <: GeneratedMessage](
      keyPair: ECKeyPair,
      did: PrismDid,
      request: R
  ): SignedRpcRequest[R] = {
    val privateKey = keyPair.getPrivateKey
    val requestNonce = UUID.randomUUID().toString.getBytes.toVector
    val signature = EC.sign(
      SignedRequestsHelper.merge(auth.model.RequestNonce(requestNonce), request.toByteArray).toArray,
      privateKey
    )
    SignedRpcRequest(
      nonce = requestNonce,
      signature = signature,
      did = did,
      keyId = PrismDid.getMASTER_KEY_ID,
      request = request
    )
  }
}
