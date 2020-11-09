package io.iohk.atala.prism.credentials.json

import io.circe.Json
import io.circe.syntax._

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.EitherValues

import io.iohk.atala.prism.credentials.json.JsonBasedCredential.JsonFields
import io.iohk.atala.prism.credentials.CredentialContent

import io.iohk.atala.prism.credentials.json.implicits._

class JsonBasedCredentialContentSpec extends AnyWordSpec with Matchers with EitherValues {

  "JsonBasedCredentialContent" should {
    "read issuer from JSON" in {
      val json = Json.obj(
        JsonFields.Issuer.name -> "did".asJson
      )

      json.as[CredentialContent[Nothing]] mustBe Right(
        CredentialContent(
          credentialType = Nil,
          issuerDid = Some("did"),
          issuanceKeyId = None,
          issuanceDate = None,
          expiryDate = None,
          credentialSubject = None
        )
      )
    }

    "read issuer from nested JSON" in {
      val json = Json.obj(
        JsonFields.Issuer.name -> Json.obj(
          JsonFields.IssuerDid.name -> "did".asJson,
          JsonFields.IssuerName.name -> "did:prism:123".asJson
        )
      )

      json.as[CredentialContent[Nothing]] mustBe Right(
        CredentialContent(
          credentialType = Nil,
          issuerDid = Some("did"),
          issuanceKeyId = None,
          issuanceDate = None,
          expiryDate = None,
          credentialSubject = None
        )
      )
    }
  }

}
