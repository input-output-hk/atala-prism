package io.iohk.atala.prism.auth.grpc

import java.util.Base64

import io.grpc.Metadata
import io.iohk.atala.prism.crypto.{ECPublicKey, ECSignature}
import io.iohk.atala.prism.auth.model.RequestNonce

sealed trait GrpcAuthenticationHeader {
  import GrpcAuthenticationContext._

  def toMetadata: Metadata = {
    val metadata = new Metadata()
    this match {
      case GrpcAuthenticationHeader.PublicKeyBased(requestNonce, publicKey, signature) =>
        val publicKeyStr = Base64.getUrlEncoder.encodeToString(publicKey.getEncoded)
        val signatureStr = Base64.getUrlEncoder.encodeToString(signature.data)
        val requestNonceStr = Base64.getUrlEncoder.encodeToString(requestNonce.bytes.toArray)
        metadata.put(PublicKeyKeys.metadata, publicKeyStr)
        metadata.put(SignatureKeys.metadata, signatureStr)
        metadata.put(RequestNonceKeys.metadata, requestNonceStr)

      case didBased: GrpcAuthenticationHeader.DIDBased =>
        val signatureStr = Base64.getUrlEncoder.encodeToString(didBased.signature.data)
        val requestNonceStr = Base64.getUrlEncoder.encodeToString(didBased.requestNonce.bytes.toArray)
        metadata.put(DidKeys.metadata, didBased.did)
        metadata.put(DidKeyIdKeys.metadata, didBased.keyId)
        metadata.put(DidSignatureKeys.metadata, signatureStr)
        metadata.put(RequestNonceKeys.metadata, requestNonceStr)
    }

    metadata
  }
}

object GrpcAuthenticationHeader {

  final case class PublicKeyBased(requestNonce: RequestNonce, publicKey: ECPublicKey, signature: ECSignature)
      extends GrpcAuthenticationHeader
  sealed trait DIDBased extends GrpcAuthenticationHeader {
    val requestNonce: RequestNonce
    val did: String
    val keyId: String
    val signature: ECSignature
  }
  final case class PublishedDIDBased(requestNonce: RequestNonce, did: String, keyId: String, signature: ECSignature)
      extends DIDBased
  final case class UnpublishedDIDBased(requestNonce: RequestNonce, did: String, keyId: String, signature: ECSignature)
      extends DIDBased
}
