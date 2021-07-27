package io.iohk.atala.mirror.models

import io.circe.generic.auto._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers

import io.iohk.atala.mirror.MirrorFixtures

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

// sbt "project mirror" "testOnly *models.RedlandIdCredentialSpec"
class RedlandIdCredentialSpec extends AnyWordSpec with Matchers with MirrorFixtures {

  import UserCredentialFixtures._

  "RedlandIdCredentialSpec" should {
    "be instantiable from credential content" in new RedlandIdCredentialFixtures {
      RedlandIdCredential.fromCredentialContent(redlandIdCredentialContent) mustBe Right(
        redlandIdCredential
      )
      RedlandIdCredential.fromCredentialContent(kycCredentialContent).left.map(_.getMessage) mustBe Left(
        "Invalid credential type (KYCCredential) for RedlandIdCredential."
      )
      KycCredential.fromCredentialContent(unknownCredentialContent).left.map(_.getMessage) mustBe Left(
        "Field not found: type"
      )
    }
  }

  "be convertable to ivms101 person" in new RedlandIdCredentialFixtures {
    redlandIdCredential.toPerson mustBe Right(
      Person(
        Person.Person.NaturalPerson(
          NaturalPerson(
            name = Some(
              NaturalPersonName(
                nameIdentifiers = Seq(
                  NaturalPersonNameId(
                    primaryIdentifier = "Jo Wong",
                    nameIdentifierType = NaturalPersonNameTypeCode.NATURAL_PERSON_NAME_TYPE_CODE_LEGL
                  )
                )
              )
            ),
            geographicAddresses = Seq.empty,
            nationalIdentification = Some(
              NationalIdentification(
                nationalIdentifier = "RL-F95B27EAD",
                nationalIdentifierType = NationalIdentifierTypeCode.NATIONAL_IDENTIFIER_TYPE_CODE_MISC
              )
            ),
            dateAndPlaceOfBirth = Some(
              DateAndPlaceOfBirth(
                dateOfBirth = "1999-01-11"
              )
            )
          )
        )
      )
    )
  }

  trait RedlandIdCredentialFixtures {
    val redlandIdCredential = RedlandIdCredential("unknown", "RL-F95B27EAD", "Jo Wong", "1999-01-11")
  }

}
