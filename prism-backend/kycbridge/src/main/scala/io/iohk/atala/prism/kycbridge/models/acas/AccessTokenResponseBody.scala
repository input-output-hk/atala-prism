package io.iohk.atala.prism.kycbridge.models.acas

case class AccessTokenResponseBody(
    accessToken: String,
    tokenType: String,
    expiresIn: Int
)
