package io.iohk.atala.prism.crypto

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ECSignatureSpec extends AnyWordSpec with Matchers {

  "ECSignature" should {
    "be convertable to P1363" in new Fixtures {
      ECSignature(der).toP1363 mustBe ECSignature(p1363)
    }

    "be convertable to ASN.1/DER" in new Fixtures {
      ECSignature(p1363).toDer mustBe ECSignature(der)
    }
  }

  trait Fixtures {
    // Sigranture created by our ECTrait implementation, it contains 64 bytes, and encoded R and S value.
    val der = Array[Byte](48, 68, 2, 32, 32, -10, 20, 89, 108, -20, -115, 6, -35, 10, -113, -5, 62, 86, 55, 127, 85,
      -87, 116, -114, 61, 115, -118, -123, -90, -15, 50, -100, 41, -105, -69, -100, 2, 32, 71, 25, -14, -18, 107, 125,
      77, 26, 1, 106, -79, 68, -19, -11, 38, 127, -96, 105, 44, -117, 125, 114, -4, 86, -90, 8, -46, -74, 25, 34, -110,
      33)

    // P1363 signature with R and S without separator.
    val p1363 = Array[Byte](32, -10, 20, 89, 108, -20, -115, 6, -35, 10, -113, -5, 62, 86, 55, 127, 85, -87, 116, -114,
      61, 115, -118, -123, -90, -15, 50, -100, 41, -105, -69, -100, 71, 25, -14, -18, 107, 125, 77, 26, 1, 106, -79, 68,
      -19, -11, 38, 127, -96, 105, 44, -117, 125, 114, -4, 86, -90, 8, -46, -74, 25, 34, -110, 33)

  }
}
