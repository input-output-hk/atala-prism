package io.iohk.cef.crypto.certificates

import org.scalatest.MustMatchers._
import org.scalatest.OptionValues._
import org.scalatest.WordSpec

class X509CertificateExtSpec extends WordSpec {

  import test.data.ExampleCertificates._

  "commonName" should {
    "return the CN" in {
      val cert = decodeX509(singleCertWithCommonNamePEM)
      val result = cert.commonName

      result.value must be("valid")
    }

    "return None when the certificate doesn't have a CN" in {
      val cert = decodeX509(singleCertWithoutCommonNamePEM)
      val result = cert.commonName

      result must be(empty)
    }
  }

  "signingPublicKey" should {
    "return the public key" in {
      val cert = decodeX509(singleCertWithCommonNamePEM)
      val result = cert.signingPublicKey

      result.isDefined must be(true)
    }
  }
}
