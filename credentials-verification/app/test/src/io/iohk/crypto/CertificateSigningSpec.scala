package io.iohk.crypto

import java.net.URI
import java.time.LocalDateTime
import java.util.Base64

import io.iohk.atala.prism.crypto.ECConfig.CURVE_NAME
import io.iohk.claims.json._
import io.iohk.claims.{Certificate, SubjectClaims}
import io.iohk.atala.prism.crypto.EC
import org.bouncycastle.jce.ECNamedCurveTable
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CertificateSigningSpec extends AnyWordSpec with Matchers {

  implicit val certificateEncoding: SignableEncoding[Certificate] = new TwoLineJsonEncoding[Certificate]

  val urlBase64EncodedD = "avwoe7yP0B58wMp7sALpCToCnA6gD2Dsv5bnScWzOL0"
  val dBytes = Base64.getUrlDecoder.decode(urlBase64EncodedD)
  val ecParameterSpec = ECNamedCurveTable.getParameterSpec(CURVE_NAME)
  val bigInteger = BigInt(1, dBytes).bigInteger
  val ecPoint = ecParameterSpec.getG.multiply(bigInteger).normalize()

  val privateKey = EC.toPrivateKey(bigInteger)
  val publicKey = EC.toPublicKey(ecPoint.getXCoord.toBigInteger, ecPoint.getYCoord.toBigInteger)

  val certificate = Certificate(
    URI.create("did:example:university"),
    LocalDateTime.now(),
    SubjectClaims(URI.create("did:example:student"), Map("degree" -> "Masters Degree in Astrology")),
    None
  )

  "CertificateSigning" should {
    "verify properly signed certificate" in {
      val encoded = CertificateSigning.sign(certificate, URI.create("did:example:university/keys/1"), privateKey)
      CertificateSigning.verify(encoded, publicKey) shouldBe true
    }

    "not verify certificate when signature doesn't match the key" in {
      val otherPublicKey = EC.generateKeyPair().publicKey
      val encoded = CertificateSigning.sign(certificate, URI.create("did:example:university/keys/1"), privateKey)
      CertificateSigning.verify(encoded, otherPublicKey) shouldBe false
    }

    "throw an exception when proof info missing" in {
      val enclosure = certificateEncoding.enclose(certificate)
      val signature = Array.fill(72)(0.toByte)
      val encoded = certificateEncoding.compose(enclosure, signature)

      an[IllegalArgumentException] should be thrownBy {
        CertificateSigning.verify(encoded, publicKey)
      }
    }
  }
}
