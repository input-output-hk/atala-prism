package io.iohk.atala.prism.credentials.json

import io.circe.parser
import io.circe.syntax._

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.EitherValues

import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.CredentialContent._
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.credentials.json.implicits._

class JsonCredentialContentSpec extends AnyWordSpec with Matchers with EitherValues {

  "CredentialContent" should {
    "be convertable to json" in new Fixtures {
      parser.parse(json) mustBe Right(credentialContent.asJson)
    }

    "be restorable from json in the same order" in new Fixtures {
      parser.decode[CredentialContent](json) mustBe Right(credentialContent)
    }

    "be restorable from empty json but fail for empty string" in new Fixtures {
      parser.decode[CredentialContent]("{}") mustBe Right(CredentialContent.empty)
      parser.decode[CredentialContent]("").isLeft mustBe true // empty string isn't a valid JSON
    }
  }

  trait Fixtures {
    val credentialContent = CredentialContent(
      "credentialType" -> Values(1, 2, 3, "test"),
      "issuerDid" -> "did",
      "issuanceKeyId" -> 123,
      "credentialSubject" -> Fields("field" -> 1)
    )

    val json = s"""{
                | "credentialType" : [
                |    1,
                |    2,
                |    3,
                |    "test"
                |  ],
                |  "issuerDid" : "did",
                |  "issuanceKeyId" : 123,
                |  "credentialSubject" : {
                |    "field" : 1
                |  }
                |}""".stripMargin
  }

}
