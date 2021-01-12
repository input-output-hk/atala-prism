package io.iohk.atala.mirror.models

import io.circe.{parser, Decoder}

import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.CredentialContent.CredentialContentException
import io.iohk.atala.prism.credentials.content.CredentialContent.WrongTypeException

case class RedlandIdCredential(
    id: String,
    identityNumber: String,
    name: String,
    dateOfBirth: String
)

object RedlandIdCredential {
  def fromCredentialContent(
      content: CredentialContent
  )(implicit d: Decoder[RedlandIdCredential]): Either[CredentialContentException, RedlandIdCredential] =
    content.credentialSubject.flatMap(subject =>
      parser.decode[RedlandIdCredential](subject).left.map(e => WrongTypeException(e.getMessage))
    )
}
