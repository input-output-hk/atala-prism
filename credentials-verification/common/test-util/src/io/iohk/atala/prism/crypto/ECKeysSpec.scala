package io.iohk.atala.prism.crypto

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ECKeys._

class ECKeysSpec extends AnyWordSpec with Matchers {
  "ECKeys.EncodePublicKey" should {
    "Encode and decode public key" in {
      val publicKey = generateKeyPair().getPublic
      val encodedPublicKey = toEncodedPublicKey(publicKey)
      val decodedPublicKey = toPublicKey(encodedPublicKey)
      publicKey mustBe decodedPublicKey
    }
  }

}
