package io.iohk.atala.prism.intdemo

import io.circe.parser.parse
import io.iohk.atala.prism.utils.Base64Utils
import io.iohk.atala.prism.protos.credential_models

case class EmploymentCredentialData private (
    employerName: String,
    employerAddress: String
)

object EmploymentCredentialData {
  def apply(
      protobufCredential: credential_models.PlainTextCredential
  ): EmploymentCredentialData = {
    val decodedCredential =
      Base64Utils.decodeUrlToString(protobufCredential.encodedCredential)
    parse(decodedCredential)
      .flatMap { json =>
        val cursor = json.hcursor
        for {
          employerName <- cursor.downField("issuerName").as[String]
          employerAddress <- cursor.downField("issuerAddress").as[String]
        } yield EmploymentCredentialData(employerName, employerAddress)
      }
      .getOrElse(
        throw new IllegalStateException(
          s"The shared employment credential is invalid. Document follows: '${decodedCredential}''"
        )
      )
  }
}
