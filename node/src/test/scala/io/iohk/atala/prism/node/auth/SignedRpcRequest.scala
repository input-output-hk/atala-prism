package io.iohk.atala.prism.node.auth

import io.iohk.atala.prism.node.auth
import io.iohk.atala.prism.node.identity.{PrismDid => DID}
import io.iohk.atala.prism.node.crypto.CryptoTestUtils.SecpPair
import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpECDSA, SecpECDSASignature}
import scalapb.GeneratedMessage

import java.util.UUID

final case class SignedRpcRequest[R <: GeneratedMessage](
    nonce: Vector[Byte],
    signature: SecpECDSASignature,
    did: DID,
    keyId: String,
    request: R
)

object SignedRpcRequest {
  def generate[R <: GeneratedMessage](
      keyPair: SecpPair,
      did: DID,
      request: R
  ): SignedRpcRequest[R] = {
    val privateKey = keyPair.privateKey
    val requestNonce = UUID.randomUUID().toString.getBytes.toVector
    val signature = SecpECDSA.signBytes(
      auth.model.RequestNonce(requestNonce).mergeWith(request.toByteArray).toArray,
      privateKey
    )
    SignedRpcRequest(
      nonce = requestNonce,
      signature = signature,
      did = did,
      keyId = DID.DEFAULT_MASTER_KEY_ID,
      request = request
    )
  }
}
