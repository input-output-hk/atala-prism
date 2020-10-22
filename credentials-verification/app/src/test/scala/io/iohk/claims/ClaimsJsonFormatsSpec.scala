package io.iohk.claims

import java.net.URI
import java.time.{LocalDateTime, ZoneId}

import io.iohk.claims.json._
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.{Format, Json}

class ClaimsJsonFormatsSpec extends AnyWordSpec with Matchers with ScalaCheckDrivenPropertyChecks {
  val json =
    """
     |{
     |  "issuer": "did:example:university",
     |  "issuanceDate": "2019-08-07T13:55:50",
     |  "credentialSubject": {
     |    "degree": "Masters Degree in Astrology",
     |    "id": "did:example:student"
     |  },
     |  "proof": {
     |    "type": "Secp256k1VerificationKey2018",
     |    "created": "2019-08-07T13:55:50",
     |    "verificationMethod": "did:example:university/keys/1"
     |  }
     |}
   """.stripMargin

  def expectedCertificate =
    Certificate(
      new URI("did:example:university"),
      LocalDateTime.of(2019, 8, 7, 13, 55, 50),
      SubjectClaims(
        new URI("did:example:student"),
        Map("degree" -> "Masters Degree in Astrology")
      ),
      Some(
        CertificateProof(
          "Secp256k1VerificationKey2018",
          LocalDateTime.of(2019, 8, 7, 13, 55, 50),
          new URI("did:example:university/keys/1")
        )
      )
    )

  "Format[Certificate]" should {
    "parse json certificate correctly" in {
      Json.parse(json).as[Certificate] shouldBe expectedCertificate
    }

    "encode arbitrary value to json that will decode to the same value" in {
      val nonEmptyStrGen = Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString)

      val didGen = for {
        method <- nonEmptyStrGen
        identifier <- nonEmptyStrGen
      } yield new URI(s"did:$method:$identifier")

      val dateTimeGen = Gen.calendar.map(cal => LocalDateTime.ofInstant(cal.toInstant, ZoneId.systemDefault()))

      def keyForDid(did: URI) = Gen.alphaStr.map(name => did.resolve(s"/keys/$name"))

      val subjectClaimsGen = for {
        id <- didGen
        properties <- Gen.nonEmptyMap(Gen.zip(nonEmptyStrGen, nonEmptyStrGen)).filterNot(_.contains("id"))
      } yield SubjectClaims(id, properties)

      def certificateProofGen(issuer: URI) =
        for {
          typ <- nonEmptyStrGen
          created <- dateTimeGen
          keyId <- keyForDid(issuer)
        } yield CertificateProof(typ, created, keyId)

      val certificateGen = for {
        issuer <- didGen
        issuanceDate <- dateTimeGen
        subjectClaims <- subjectClaimsGen
        proof <- Gen.option(certificateProofGen(issuer))
      } yield Certificate(issuer, issuanceDate, subjectClaims, proof)

      forAll(certificateGen) { (certificate: Certificate) =>
        implicitly[Format[Certificate]].writes(certificate).as[Certificate] shouldBe certificate
      }
    }
  }
}
