package io.iohk.atala.prism.node.auth.grpc

import java.util.Base64
import io.grpc.Metadata
import io.iohk.atala.prism.node.auth.model.RequestNonce
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpECDSASignature, SecpPublicKey}

sealed trait GrpcAuthenticationHeader {
  import GrpcAuthenticationContext._

  def toMetadata: Metadata = {
    val metadata = new Metadata()
    this match {
      case GrpcAuthenticationHeader.PublicKeyBased(
            requestNonce,
            publicKey,
            signature
          ) =>
        val publicKeyStr =
          Base64.getUrlEncoder.encodeToString(publicKey.unCompressed)
        val signatureStr =
          Base64.getUrlEncoder.encodeToString(signature.bytes)
        val requestNonceStr =
          Base64.getUrlEncoder.encodeToString(requestNonce.bytes.toArray)
        metadata.put(PublicKeyKeys.metadata, publicKeyStr)
        metadata.put(SignatureKeys.metadata, signatureStr)
        metadata.put(RequestNonceKeys.metadata, requestNonceStr)

      case didBased: GrpcAuthenticationHeader.DIDBased =>
        val signatureStr =
          Base64.getUrlEncoder.encodeToString(didBased.signature.bytes)
        val requestNonceStr = Base64.getUrlEncoder.encodeToString(
          didBased.requestNonce.bytes.toArray
        )
        metadata.put(DidKeys.metadata, didBased.did.getValue)
        metadata.put(DidKeyIdKeys.metadata, didBased.keyId)
        metadata.put(DidSignatureKeys.metadata, signatureStr)
        metadata.put(RequestNonceKeys.metadata, requestNonceStr)
    }

    metadata
  }
}

object GrpcAuthenticationHeader {

  final case class PublicKeyBased(
      requestNonce: RequestNonce,
      publicKey: SecpPublicKey,
      signature: SecpECDSASignature
  ) extends GrpcAuthenticationHeader
  sealed trait DIDBased extends GrpcAuthenticationHeader {
    val requestNonce: RequestNonce
    val did: DID
    val keyId: String
    val signature: SecpECDSASignature
  }
  final case class PublishedDIDBased(
      requestNonce: RequestNonce,
      did: DID,
      keyId: String,
      signature: SecpECDSASignature
  ) extends DIDBased
  final case class UnpublishedDIDBased(
      requestNonce: RequestNonce,
      did: DID,
      keyId: String,
      signature: SecpECDSASignature
  ) extends DIDBased
}
