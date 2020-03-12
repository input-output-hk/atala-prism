package io.iohk.cvp.intdemo

import java.time.LocalDate

import io.circe
import io.circe.parser._

case class IdData private (name: String, dob: LocalDate)

object IdData {
  def toIdData(protobufCredential: credential.Credential): Either[circe.Error, IdData] = {
    parse(protobufCredential.credentialDocument)
      .flatMap { json =>
        val cursor = json.hcursor.downField("credentialSubject")
        for {
          name <- cursor.downField("name").as[String]
          dob <- cursor.downField("dateOfBirth").as[LocalDate]
        } yield IdData(name, dob)
      }
  }
}
