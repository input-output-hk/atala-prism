package io.iohk.atala.prism.auth

import io.iohk.atala.prism.auth
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.ECKeyPair
import io.iohk.atala.prism.crypto.signature.ECSignature
import io.iohk.atala.prism.identity.{PrismDid => DID}
import scalapb.GeneratedMessage

import java.util.UUID

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
      auth.model.RequestNonce(requestNonce).mergeWith(request.toByteArray).toArray,
      privateKey
    )
    SignedRpcRequest(
      nonce = requestNonce,
      signature = signature,
      did = did,
      keyId = DID.getDEFAULT_MASTER_KEY_ID,
      request = request
    )
  }
}
