package io.iohk.atala.mirror.models

import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.CredentialContent.CredentialContentException
import io.iohk.atala.mirror.models.UserCredential.UserCredentialException
import io.iohk.atala.mirror.protos.ivms101.{
  DateAndPlaceOfBirth,
  NationalIdentification,
  NationalIdentifierTypeCode,
  NaturalPerson,
  NaturalPersonName,
  NaturalPersonNameId,
  NaturalPersonNameTypeCode,
  Person
}

case class KycCredential(
    content: CredentialContent
) {
  def toPerson: Either[UserCredentialException, Person] = {
    val credentialSubjectField = CredentialContent.JsonFields.CredentialSubject.field
    for {
      naturalPersonName <-
        content
          .getString(s"$credentialSubjectField.name")
          .map(name =>
            NaturalPersonName(
              nameIdentifiers = Seq(
                NaturalPersonNameId(
                  primaryIdentifier = name,
                  nameIdentifierType = NaturalPersonNameTypeCode.NATURAL_PERSON_NAME_TYPE_CODE_LEGL
                )
              )
            )
          )
          .left
          .map(error => UserCredentialException(error.getMessage))

      nationalIdentification <-
        content
          .getString(s"$credentialSubjectField.idDocument.personalNumber")
          .map(identityNumber =>
            NationalIdentification(
              nationalIdentifier = identityNumber,
              nationalIdentifierType = NationalIdentifierTypeCode.NATIONAL_IDENTIFIER_TYPE_CODE_MISC
            )
          )
          .left
          .map(error => UserCredentialException(error.getMessage))

      dateAndPlaceOfBirth <-
        content
          .getString(s"$credentialSubjectField.birthDate")
          .map(dateOfBirth =>
            DateAndPlaceOfBirth(
              dateOfBirth = dateOfBirth
            )
          )
          .left
          .map(error => UserCredentialException(error.getMessage))
    } yield Person().withNaturalPerson(
      NaturalPerson(
        name = Some(naturalPersonName),
        geographicAddresses = Seq.empty,
        nationalIdentification = Some(nationalIdentification),
        dateAndPlaceOfBirth = Some(dateAndPlaceOfBirth)
      )
    )
  }
}

object KycCredential {

  lazy val KYC_CREDENTIAL_TYPE = "KYCCredential"

  def fromCredentialContent(
      content: CredentialContent
  ): Either[CredentialContentException, KycCredential] =
    content.credentialType.flatMap {
      case KYC_CREDENTIAL_TYPE :: Nil => Right(KycCredential(content))
      case typeField =>
        Left(
          new CredentialContentException(s"Invalid credential type (${typeField.mkString(", ")}) for KycCredential.")
        )
    }
}
