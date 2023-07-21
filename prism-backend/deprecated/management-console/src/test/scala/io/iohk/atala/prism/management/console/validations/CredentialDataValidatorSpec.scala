package io.iohk.atala.prism.management.console.validations

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNel
import io.circe.Json
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.management.console.DataPreparation
import io.iohk.atala.prism.management.console.models.CredentialTypeFieldType
import io.iohk.atala.prism.management.console.validations.CredentialDataValidationError.{
  FieldInvalidType,
  FieldMissing,
  InvalidDateFormat,
  NotJsonObject
}
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers

// sbt "project management-console" "testOnly *CredentialDataValidatorSpec"
class CredentialDataValidatorSpec extends AtalaWithPostgresSpec with Matchers {

  "validate" should {
    "return valid ValidatedNel" in {
      val institutionId = DataPreparation.createParticipant("Participant")
      val credentialTypeWithRequiredFields =
        DataPreparation.createCredentialType(institutionId, "name")

      val validCredentialData = Json.obj(
        "title" -> Json.fromString("Major IN Applied Blockchain"),
        "enrollmentDate" -> Json.fromString("01/10/2010"),
        "graduationDate" -> Json.fromString("01/07/2015")
      )

      CredentialDataValidator
        .validate(credentialTypeWithRequiredFields, validCredentialData)
        .isValid mustBe true
    }

    "return invalid ValidatedNel when credential data is not Json object" in {
      val institutionId = DataPreparation.createParticipant("Participant")
      val credentialTypeWithRequiredFields =
        DataPreparation.createCredentialType(institutionId, "name")

      check(
        CredentialDataValidator.validate(
          credentialTypeWithRequiredFields,
          Json.fromString("credential")
        ),
        NotJsonObject
      )
    }

    "return invalid ValidatedNel when field is missing" in {
      val institutionId = DataPreparation.createParticipant("Participant")
      val credentialTypeWithRequiredFields =
        DataPreparation.createCredentialType(institutionId, "name")

      val validCredentialData = Json.obj(
        "enrollmentDate" -> Json.fromString("01/10/2010"),
        "graduationDate" -> Json.fromString("01/07/2015")
      )

      check(
        CredentialDataValidator
          .validate(credentialTypeWithRequiredFields, validCredentialData),
        FieldMissing("title")
      )
    }

    "return invalid ValidatedNel when field type doesn't match" in {
      val institutionId = DataPreparation.createParticipant("Participant")
      val credentialTypeWithRequiredFields =
        DataPreparation.createCredentialType(institutionId, "name")

      val validCredentialData = Json.obj(
        "title" -> Json.fromString("Major IN Applied Blockchain"),
        "enrollmentDate" -> Json.fromString("01/10/2010"),
        "graduationDate" -> Json.fromBoolean(false)
      )

      check(
        CredentialDataValidator
          .validate(credentialTypeWithRequiredFields, validCredentialData),
        FieldInvalidType(
          "graduationDate",
          CredentialTypeFieldType.Date,
          "false"
        )
      )
    }

    "return invalid ValidatedNel when date is in incorrect format" in {
      val institutionId = DataPreparation.createParticipant("Participant")
      val credentialTypeWithRequiredFields =
        DataPreparation.createCredentialType(institutionId, "name")

      val validCredentialData = Json.obj(
        "title" -> Json.fromString("Major IN Applied Blockchain"),
        "enrollmentDate" -> Json.fromString("01/10/2010"),
        "graduationDate" -> Json.fromString("01/07-2015")
      )

      check(
        CredentialDataValidator
          .validate(credentialTypeWithRequiredFields, validCredentialData),
        InvalidDateFormat("graduationDate", "\"01/07-2015\"")
      )
    }
  }

  private def check(
      result: ValidatedNel[CredentialDataValidationError, Unit],
      expectedError: CredentialDataValidationError
  ): Assertion =
    result match {
      case Invalid(errors) => errors.head mustBe expectedError
      case Valid(_) => fail("Validation result should be invalid but is valid")
    }

}
