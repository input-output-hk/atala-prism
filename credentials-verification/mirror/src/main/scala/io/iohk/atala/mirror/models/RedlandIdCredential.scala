package io.iohk.atala.mirror.models

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import io.iohk.atala.prism.credentials.CredentialContent
import io.iohk.atala.prism.credentials.json.implicits._

case class RedlandIdCredential(
    id: String,
    identityNumber: String,
    name: String,
    dateOfBirth: String
)

object RedlandIdCredential {
  implicit val redlandIdCredentialDecoder: Decoder[RedlandIdCredential] = deriveDecoder[RedlandIdCredential]
  implicit val redlandIdCredentialEncoder: Encoder[RedlandIdCredential] = deriveEncoder[RedlandIdCredential]

  implicit val redlandIdCredentialContentDecoder: Decoder[CredentialContent[RedlandIdCredential]] =
    decodeCredentialContent[RedlandIdCredential]

  implicit val redlandIdCredentialContentEncoder: Encoder[CredentialContent[RedlandIdCredential]] =
    encodeCredentialContent[RedlandIdCredential]
}
