package io.iohk.atala.prism.management.console.clients

import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeader
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import io.iohk.atala.prism.kotlin.identity.{PrismDid => DID}

object ClientHelper {
  def requestSigner(
      authenticator: RequestAuthenticator,
      whitelistedDID: DID,
      didPrivateKey: ECPrivateKey
  ): scalapb.GeneratedMessage => GrpcAuthenticationHeader.DIDBased = { request =>
    val signedRequest = authenticator.signConnectorRequest(request.toByteArray, didPrivateKey)
    GrpcAuthenticationHeader.UnpublishedDIDBased(
      did = whitelistedDID,
      keyId = DID.getMASTER_KEY_ID,
      requestNonce = RequestNonce(signedRequest.requestNonce.toVector),
      signature = new ECSignature(signedRequest.signature)
    )
  }
}
