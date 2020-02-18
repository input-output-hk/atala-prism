package io.iohk.cvp.grpc

import java.util.Base64

import io.grpc.Metadata
import io.iohk.cvp.crypto.ECKeys.EncodedPublicKey
import io.iohk.cvp.models.ParticipantId

sealed trait GrpcAuthenticationHeader {
  import GrpcAuthenticationContext._

  def toMetadata: Metadata = {
    val metadata = new Metadata()
    this match {
      case GrpcAuthenticationHeader.Legacy(userId) =>
        metadata.put(UserIdKeys.metadata, userId.uuid.toString)

      case GrpcAuthenticationHeader.PublicKeyBased(publicKey, signature) =>
        val publicKeyStr = Base64.getUrlEncoder.encodeToString(publicKey.bytes.toArray)
        val signatureStr = Base64.getUrlEncoder.encodeToString(signature.toArray)
        metadata.put(PublicKeyKeys.metadata, publicKeyStr)
        metadata.put(SignatureKeys.metadata, signatureStr)

      case GrpcAuthenticationHeader.DIDBased(did, keyId, signature) =>
        val signatureStr = Base64.getUrlEncoder.encodeToString(signature.toArray)
        metadata.put(DidKeys.metadata, did)
        metadata.put(DidKeyIdKeys.metadata, keyId)
        metadata.put(DidSignatureKeys.metadata, signatureStr)
    }

    metadata
  }
}

object GrpcAuthenticationHeader {

  final case class Legacy(userId: ParticipantId) extends GrpcAuthenticationHeader
  final case class PublicKeyBased(publicKey: EncodedPublicKey, signature: Vector[Byte]) extends GrpcAuthenticationHeader
  final case class DIDBased(did: String, keyId: String, signature: Vector[Byte]) extends GrpcAuthenticationHeader
}
