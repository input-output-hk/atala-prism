package io.iohk.cef.crypto.certificates

import org.scalatest.MustMatchers._
import org.scalatest.OptionValues._
import org.scalatest.WordSpec

class CachedCertificatePairSpec extends WordSpec {

  import ExampleCertificates._

  "decode" should {
    "decode a valid certificate pair" in {
      val result = CachedCertificatePair.decode(twoChainedCertsPEM)

      result.isDefined must be(true)
    }

    "accept a pair even if they don't form a chain" in {
      val result = CachedCertificatePair.decode(twoUnchainedCertsPEM)

      result.isDefined must be(true)
    }

    "reject a single certificate" in {
      val result = CachedCertificatePair.decode(singleCertWithCommonNamePEM)

      result must be(empty)
    }
  }

  "from" should {
    "fail to create an instance when both identities are the same" in {
      val x509 = decodeX509(singleCertWithCommonNamePEM)
      val certificate = CachedCertificate.from(x509).value
      val result = CachedCertificatePair.from(certificate, certificate)

      result must be(empty)
    }

    "allow to create an instance when the identities are different" in {
      val pair = CachedCertificatePair.decode(twoChainedCertsPEM).value
      val result = CachedCertificatePair.from(pair.target, pair.issuer)

      result.value must be(pair)
    }
  }

  "isSignatureValid" should {
    "accept a valid chain" in {
      val pair = CachedCertificatePair.decode(twoChainedCertsPEM).value
      val result = pair.isSignatureValid

      result must be(true)
    }

    "reject an invalid chain" in {
      val pair = CachedCertificatePair.decode(twoUnchainedCertsPEM).value
      val result = pair.isSignatureValid

      result must be(false)
    }

    "ignore the notBefore date" in {
      val pair = CachedCertificatePair.decode(twoChainedCertsNotValidYetPEM).value
      val result = pair.isSignatureValid

      result must be(true)
    }

    "ignore the notAfter date" in {
      val pair = CachedCertificatePair.decode(twoChainedCertsExpiredPEM).value
      val result = pair.isSignatureValid

      result must be(true)
    }
  }
}
