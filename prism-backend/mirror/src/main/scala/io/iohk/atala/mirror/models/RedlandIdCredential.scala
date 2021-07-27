package io.iohk.atala.mirror.models

import io.circe.{parser, Decoder}

import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.CredentialContent.CredentialContentException
import io.iohk.atala.prism.credentials.content.CredentialContent.WrongTypeException
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

case class RedlandIdCredential(
    id: String,
    identityNumber: String,
    name: String,
    dateOfBirth: String
) {
  def toPerson: Either[UserCredentialException, Person] = {

    val naturalPersonName = NaturalPersonName(
      nameIdentifiers = Seq(
        NaturalPersonNameId(
          primaryIdentifier = name,
          nameIdentifierType = NaturalPersonNameTypeCode.NATURAL_PERSON_NAME_TYPE_CODE_LEGL
        )
      )
    )

    val nationalIdentification = NationalIdentification(
      nationalIdentifier = identityNumber,
      nationalIdentifierType = NationalIdentifierTypeCode.NATIONAL_IDENTIFIER_TYPE_CODE_MISC
    )

    val dateAndPlaceOfBirth = DateAndPlaceOfBirth(
      dateOfBirth = dateOfBirth
    )

    val naturalPerson = NaturalPerson(
      name = Some(naturalPersonName),
      geographicAddresses = Seq.empty,
      nationalIdentification = Some(nationalIdentification),
      dateAndPlaceOfBirth = Some(dateAndPlaceOfBirth)
    )

    Right(Person().withNaturalPerson(naturalPerson))
  }
}

object RedlandIdCredential {

  lazy val REDLAND_ID_CREDENTIAL_TYPE = Seq("VerifiableCredential", "RedlandIdCredential")

  def fromCredentialContent(
      content: CredentialContent
  )(implicit d: Decoder[RedlandIdCredential]): Either[CredentialContentException, RedlandIdCredential] =
    content.credentialType.flatMap {
      case REDLAND_ID_CREDENTIAL_TYPE =>
        content.credentialSubject.flatMap(subject =>
          parser.decode[RedlandIdCredential](subject).left.map(e => WrongTypeException(e.getMessage))
        )
      case typeField =>
        Left(
          new CredentialContentException(
            s"Invalid credential type (${typeField.mkString(", ")}) for RedlandIdCredential."
          )
        )
    }
}
