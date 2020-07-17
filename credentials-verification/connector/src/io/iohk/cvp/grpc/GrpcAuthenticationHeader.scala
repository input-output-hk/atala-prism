package io.iohk.cvp.grpc

import java.util.Base64

import io.grpc.Metadata
import io.iohk.atala.crypto.{ECPublicKey, ECSignature}
import io.iohk.connector.model.RequestNonce
import io.iohk.cvp.models.ParticipantId

sealed trait GrpcAuthenticationHeader {
  import GrpcAuthenticationContext._

  def toMetadata: Metadata = {
    val metadata = new Metadata()
    this match {
      case GrpcAuthenticationHeader.Legacy(userId) =>
        metadata.put(UserIdKeys.metadata, userId.uuid.toString)

      case GrpcAuthenticationHeader.PublicKeyBased(requestNonce, publicKey, signature) =>
        val publicKeyStr = Base64.getUrlEncoder.encodeToString(publicKey.getEncoded)
        val signatureStr = Base64.getUrlEncoder.encodeToString(signature.data)
        val requestNonceStr = Base64.getUrlEncoder.encodeToString(requestNonce.bytes.toArray)
        metadata.put(PublicKeyKeys.metadata, publicKeyStr)
        metadata.put(SignatureKeys.metadata, signatureStr)
        metadata.put(RequestNonceKeys.metadata, requestNonceStr)

      case GrpcAuthenticationHeader.DIDBased(requestNonce, did, keyId, signature) =>
        val signatureStr = Base64.getUrlEncoder.encodeToString(signature.data)
        val requestNonceStr = Base64.getUrlEncoder.encodeToString(requestNonce.bytes.toArray)
        metadata.put(DidKeys.metadata, did)
        metadata.put(DidKeyIdKeys.metadata, keyId)
        metadata.put(DidSignatureKeys.metadata, signatureStr)
        metadata.put(RequestNonceKeys.metadata, requestNonceStr)
    }

    metadata
  }
}

object GrpcAuthenticationHeader {

  final case class Legacy(userId: ParticipantId) extends GrpcAuthenticationHeader
  final case class PublicKeyBased(requestNonce: RequestNonce, publicKey: ECPublicKey, signature: ECSignature)
      extends GrpcAuthenticationHeader
  final case class DIDBased(requestNonce: RequestNonce, did: String, keyId: String, signature: ECSignature)
      extends GrpcAuthenticationHeader
}
