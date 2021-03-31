package io.iohk.atala.prism.connector.model.actions

import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeader
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.crypto.ECPublicKey

case class AddConnectionRequest(
    token: TokenString,
    publicKey: ECPublicKey,
    authHeader: Option[GrpcAuthenticationHeader]
)
