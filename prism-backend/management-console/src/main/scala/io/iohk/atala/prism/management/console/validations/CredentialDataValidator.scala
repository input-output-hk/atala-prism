package io.iohk.atala.prism.management.console.validations

import cats.data.{Validated, ValidatedNel}
import cats.data.Validated._
import cats.implicits._
import io.circe.{Json, JsonObject}
import io.iohk.atala.prism.management.console.models.{
  CredentialTypeField,
  CredentialTypeFieldType,
  CredentialTypeWithRequiredFields
}
import CredentialDataValidationError._
import tofu.logging.{DictLoggable, LogRenderer}

sealed abstract class CredentialDataValidationError(val message: String) extends Exception(message)
object CredentialDataValidationError {

  implicit val loggableCredentialDataValidationError: DictLoggable[CredentialDataValidationError] =
    new DictLoggable[CredentialDataValidationError] {
      override def fields[I, V, R, S](a: CredentialDataValidationError, i: I)(implicit
          r: LogRenderer[I, V, R, S]
      ): R =
        r.addString("CredentialDataValidationError", a.message, i)

      override def logShow(a: CredentialDataValidationError): String =
        s"CredentialDataValidationError{message=${a.message}"
    }

  case object NotJsonObject
      extends CredentialDataValidationError(
        message = s"Credential data is not a JSON object"
      )

  case class FieldMissing(fieldName: String)
      extends CredentialDataValidationError(
        message = s"Field $fieldName is required but is missing"
      )

  case class FieldInvalidType(
      fieldName: String,
      expectedFieldType: CredentialTypeFieldType,
      actualValue: String
  ) extends CredentialDataValidationError(
        message = s"Field $fieldName expected type is: $expectedFieldType but actual value was: $actualValue"
      )

  case class InvalidDateFormat(
      fieldName: String,
      actualValue: String
  ) extends CredentialDataValidationError(
        message =
          s"Field $fieldName has incorrect date format: $actualValue, allowed formats are DD/MM/YY or DD/MM/YYYY"
      )
}

object CredentialDataValidator {

  private val DATE_REGEX = "[0-9]{2}/[0-9]{2}/(([0-9]{2})|([0-9]{4}))".r

  def validate(
      credentialTypeWithRequiredFields: CredentialTypeWithRequiredFields,
      credentialData: Json
  ): ValidatedNel[CredentialDataValidationError, Unit] = {
    def validateFieldType(
        credentialTypeField: CredentialTypeField,
        jsonValue: Json
    ): ValidatedNel[CredentialDataValidationError, Unit] = {
      val typeMatch = credentialTypeField.`type` match {
        case CredentialTypeFieldType.String => jsonValue.asString.isDefined
        case CredentialTypeFieldType.Boolean => jsonValue.asBoolean.isDefined
        case CredentialTypeFieldType.Int => jsonValue.asNumber.isDefined
        case CredentialTypeFieldType.Date => jsonValue.asString.isDefined
      }

      if (typeMatch && CredentialTypeFieldType.Date == credentialTypeField.`type`) {
        Validated.condNel(
          jsonValue.asString.exists(date => DATE_REGEX.matches(date)),
          (),
          InvalidDateFormat(
            credentialTypeField.name,
            jsonValue.toString()
          )
        )
      } else {
        Validated.condNel(
          typeMatch,
          (),
          FieldInvalidType(
            credentialTypeField.name,
            credentialTypeField.`type`,
            jsonValue.toString()
          )
        )
      }
    }

    def validateJsonObject(
        jsonObject: JsonObject
    ): ValidatedNel[CredentialDataValidationError, Unit] = {

      credentialTypeWithRequiredFields.requiredFields
        .map { credentialTypeField =>
          val value = jsonObject.toMap.get(credentialTypeField.name)

          value.fold(
            ifEmpty = invalidNel[CredentialDataValidationError, Unit](
              FieldMissing(credentialTypeField.name)
            )
          )(jsonValue => validateFieldType(credentialTypeField, jsonValue))
        }
        .sequence
        .void
    }

    credentialData.asObject
      .fold(
        Validated.invalidNel[CredentialDataValidationError, Unit](NotJsonObject)
      )(validateJsonObject)
  }
}
