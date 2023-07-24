package io.iohk.atala.prism.connector.model.actions

import cats.syntax.either._
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeader
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.identity.{PrismDid => DID}

case class AddConnectionRequest(
    token: TokenString,
    basedOn: Either[
      UnpublishedDidBasedAddConnectionRequest,
      PublicKeyBasedAddConnectionRequest
    ]
) {
  def didOrPublicKey: Either[DID, ECPublicKey] =
    basedOn.bimap(_.authHeader.did, _.publicKey)
}

final case class PublicKeyBasedAddConnectionRequest(
    token: TokenString,
    publicKey: ECPublicKey,
    authHeader: GrpcAuthenticationHeader.PublicKeyBased
)

final case class UnpublishedDidBasedAddConnectionRequest(
    token: TokenString,
    authHeader: GrpcAuthenticationHeader.UnpublishedDIDBased
)
