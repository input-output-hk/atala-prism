package io.iohk.atala.prism.node.nonce

import io.iohk.atala.prism.node.auth.grpc.GrpcAuthenticationHeader
import io.iohk.atala.prism.node.auth.model.RequestNonce
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpECDSASignature, SecpPrivateKey}

object ClientHelper {
  def requestSigner(
      authenticator: RequestAuthenticator,
      whitelistedDID: DID,
      didPrivateKey: SecpPrivateKey
  ): scalapb.GeneratedMessage => GrpcAuthenticationHeader.DIDBased = { request =>
    val signedRequest =
      authenticator.signConnectorRequest(request.toByteArray, didPrivateKey)
    GrpcAuthenticationHeader.UnpublishedDIDBased(
      did = whitelistedDID,
      keyId = DID.getDEFAULT_MASTER_KEY_ID,
      requestNonce = RequestNonce(signedRequest.requestNonce.toVector),
      signature = SecpECDSASignature.fromBytes(signedRequest.signature)
    )
  }
}
