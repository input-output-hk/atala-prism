package io.iohk.atala.prism.crypto

import org.scalatest.{MustMatchers, WordSpec}
import ECKeys._
class ECKeysSpec extends WordSpec with MustMatchers {
  "ECKeys.EncodePublicKey" should {
    "Encode and decode public key" in {
      val publicKey = generateKeyPair().getPublic
      val encodedPublicKey = toEncodedPublicKey(publicKey)
      val decodedPublicKey = toPublicKey(encodedPublicKey)
      publicKey mustBe decodedPublicKey
    }
  }

}
