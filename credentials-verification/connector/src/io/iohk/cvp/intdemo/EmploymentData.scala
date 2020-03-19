package io.iohk.cvp.intdemo

import io.circe.parser.parse

case class EmploymentData private (employerName: String, employerAddress: String)

object EmploymentData {
  def toEmploymentData(protobufCredential: credential.Credential): EmploymentData = {
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
