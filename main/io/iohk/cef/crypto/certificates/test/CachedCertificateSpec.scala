package io.iohk.cef.crypto.certificates

import io.iohk.cef.crypto._
import org.scalatest.MustMatchers._
import org.scalatest.OptionValues._
import org.scalatest.WordSpec

class CachedCertificateSpec extends WordSpec {

  import test.data.ExampleCertificates._

  val x509 = decodeX509(singleCertWithCommonNamePEM)

  "from" should {
    "allow creating an instance when the certificate matches the identity and public key" in {
      val result = CachedCertificate.from(x509.commonName.value, x509.signingPublicKey.value, x509)

      result.isDefined must be(true)
      result.value must be(CachedCertificate.from(x509).value)
    }

    "fail to create an instance if the identity doesn't match the one from the x509 certificate" in {
      val result = CachedCertificate.from("asda", x509.signingPublicKey.value, x509)

      result must be(empty)
    }

    "fail to create an instance if the public key doesn't match the one from the x509 certificate" in {
      val key = generateSigningKeyPair().public
      val result = CachedCertificate.from(x509.commonName.value, key, x509)

      result must be(empty)
    }
  }

  "x509Codec" should {

    val x509 = decodeX509(singleCertWithCommonNamePEM)
    "decode an encoded value" in {
      val encoded = CachedCertificate.x509Codec.encode(x509)
      val decoded = CachedCertificate.x509Codec.decode(encoded)

      decoded.value must be(x509)
    }
  }
}
