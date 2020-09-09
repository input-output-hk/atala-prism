package io.iohk.atala.prism.intdemo

import io.circe.parser.parse
import io.iohk.prism.protos.credential_models

case class EmploymentData private (employerName: String, employerAddress: String)

object EmploymentData {
  def toEmploymentData(protobufCredential: credential_models.Credential): EmploymentData = {
    parse(protobufCredential.credentialDocument)
      .flatMap { json =>
        val cursor = json.hcursor.downField("issuer")
        for {
          employerName <- cursor.downField("name").as[String]
          employerAddress <- cursor.downField("address").as[String]
        } yield EmploymentData(employerName, employerAddress)
      }
      .getOrElse(
        throw new IllegalStateException(
          s"The shared employment credential is invalid. Document follows: '${protobufCredential.credentialDocument}''"
        )
      )
  }
}
