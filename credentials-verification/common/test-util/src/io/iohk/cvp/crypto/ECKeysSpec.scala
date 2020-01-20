package io.iohk.cvp.crypto

import org.scalatest.{MustMatchers, WordSpec}
import ECKeys._
class ECKeysSpec extends WordSpec with  MustMatchers{
  "Encode and decode public key" should {
    "work" in {
      val publicKey = generateKeyPair().getPublic
      val ecPoint = getECPoint(publicKey)
      val encodeBytes = toEncodePublicKey(ecPoint.getAffineX,ecPoint.getAffineY)
      val decodedEcPoint = toJavaECPoint(encodeBytes)
      ecPoint mustBe decodedEcPoint
    }
  }

}
