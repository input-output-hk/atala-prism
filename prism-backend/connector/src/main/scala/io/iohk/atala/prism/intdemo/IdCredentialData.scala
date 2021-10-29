package io.iohk.atala.prism.intdemo

import java.time.LocalDate
import io.iohk.atala.prism.utils.Base64Utils
import io.circe.parser._
import io.iohk.atala.prism.protos.credential_models

case class IdCredentialData private (name: String, dateOfBirth: LocalDate)

object IdCredentialData {
  def apply(
      protobufCredential: credential_models.PlainTextCredential
  ): IdCredentialData = {
    val decodedCredential =
      Base64Utils.decodeUrlToString(protobufCredential.encodedCredential)
    parse(decodedCredential)
      .flatMap { json =>
        val cursor = json.hcursor.downField("credentialSubject")
        for {
          name <- cursor.downField("name").as[String]
          dateOfBirth <- cursor.downField("dateOfBirth").as[LocalDate]
        } yield IdCredentialData(name, dateOfBirth)
      }
      .getOrElse(
        throw new IllegalStateException(
          s"The shared id credential is invalid. Document follows: '${decodedCredential}''"
        )
      )
  }
}
