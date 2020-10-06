package io.iohk.atala.identity

import io.iohk.atala.crypto.ECTrait
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec

abstract class DIDSpecBase(val ec: ECTrait) extends AnyWordSpec {

  "DID" should {
    "create the expected long form DID" in {
      // bytes extracted from a randomly generated key
      val xBytes = Array[Byte](30, -105, -2, -54, 18, 39, -77, -30, -49, -34, -46, -17, 3, -56, -100, 11, -111, 73, -43,
        -103, 65, -63, -71, 101, 62, -46, 29, 53, -99, -22, -46, 53)
      val yBytes = Array[Byte](0, -103, 81, -25, 85, 91, -109, -113, 111, 106, 7, -95, 3, 4, 36, 22, -11, -65, 126, -4,
        -116, -42, -90, -72, -118, 87, -120, 17, -119, 23, -77, -118, 69)
      val masterKey = ec.toPublicKey(xBytes, yBytes)

      // The expected resulting DID
      val expectedDID =
        "did:prism:0f753f41e0f3488ba56bd581d153ae9b3c9040cbcc7a63245b4644a265eb3b77:CmEKXxJdCgdtYXN0ZXIwEAFCUAoJc2VjcDI1NmsxEiAel_7KEiez4s_e0u8DyJwLkUnVmUHBuWU-0h01nerSNRohAJlR51Vbk49vagehAwQkFvW_fvyM1qa4ileIEYkXs4pF"

      DID.createUnpublishedDID(masterKey) mustBe expectedDID
    }
  }
}
