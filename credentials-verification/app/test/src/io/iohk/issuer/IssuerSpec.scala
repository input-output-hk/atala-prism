package io.iohk.issuer

import java.net.URI
import java.time.LocalDateTime
import java.util.Base64

import io.iohk.claims.json._
import io.iohk.claims.{Certificate, SubjectClaims}
import io.iohk.crypto.{ECKeys, SignableEncoding, TwoLineJsonEncoding}
import io.iohk.dids.DIDLoader
import org.scalatest.OptionValues._
import org.scalatest.{Matchers, WordSpec}

class IssuerSpec extends WordSpec with Matchers {

  implicit val certificateEncoding: SignableEncoding[Certificate] = new TwoLineJsonEncoding[Certificate]
  implicit val dateTimeOrdering = new Ordering[LocalDateTime] {
    override def compare(x: LocalDateTime, y: LocalDateTime): Int = x.compareTo(y)
  }

  val issuerDidDocument = DIDLoader.getDID(os.resource / "issuer" / "did.json").get
  val urlBase64EncodedD = "avwoe7yP0B58wMp7sALpCToCnA6gD2Dsv5bnScWzOL0"
  val dBytes = Base64.getUrlDecoder.decode(urlBase64EncodedD)
  val publicKey = ECKeys.toPublicKey(dBytes)
  val privateKey = ECKeys.toPrivateKey(dBytes)

  val claims = SubjectClaims(URI.create("did:example:student"), Map("degree" -> "Masters Degree in Astrology"))

  "Issuer" should {
    "issue a credential given private key matching DID document" in {
      val beforeSigningTime = LocalDateTime.now()
      val signedString = Issuer.sign(claims, privateKey, publicKey, issuerDidDocument)
      val afterSigningTime = LocalDateTime.now()

      val (enclosing, _) = certificateEncoding.decompose(signedString)
      val signed = certificateEncoding.disclose(enclosing)

      signed.issuer.toString shouldBe issuerDidDocument.id

      signed.proof.value.verificationMethod shouldBe new URI("#key-1")
      signed.proof.value.`type` shouldBe "Secp256k1VerificationKey2018"

      signed.issuanceDate should be >= beforeSigningTime
      signed.proof.value.created should be >= signed.issuanceDate
      signed.proof.value.created should be <= afterSigningTime

      signed.proof.value.created should be >= beforeSigningTime
    }

    "throw an expection when trying to sign with key unknown to DID document" in {
      val keys = ECKeys.generateKeyPair()

      an[IllegalArgumentException] shouldBe thrownBy {
        Issuer.sign(claims, keys.getPrivate, keys.getPublic, issuerDidDocument)
      }
    }

  }

}
