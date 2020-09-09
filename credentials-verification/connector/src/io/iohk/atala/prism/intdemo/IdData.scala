package io.iohk.atala.prism.intdemo

import java.time.LocalDate

import io.circe.parser._
import io.iohk.prism.protos.credential_models

case class IdData private (name: String, dob: LocalDate)

object IdData {
  def toIdData(protobufCredential: credential_models.Credential): IdData = {
    parse(protobufCredential.credentialDocument)
      .flatMap { json =>
        val cursor = json.hcursor.downField("credentialSubject")
        for {
          name <- cursor.downField("name").as[String]
          dob <- cursor.downField("dateOfBirth").as[LocalDate]
        } yield IdData(name, dob)
      }
      .getOrElse(
        throw new IllegalStateException(
          s"The shared id credential is invalid. Document follows: '${protobufCredential.credentialDocument}''"
        )
      )
  }
}
