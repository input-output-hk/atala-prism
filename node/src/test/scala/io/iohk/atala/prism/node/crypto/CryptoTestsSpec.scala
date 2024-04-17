package io.iohk.atala.prism.node.crypto

import io.iohk.atala.prism.crypto.EC
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec

class CryptoTestsSpec extends AnyWordSpec {

  "crypto library" should {
    "Must generate the same key from different encodings" in {
      val pair = EC.INSTANCE.generateKeyPair()
      val compressedPub = pair.getPublicKey.getEncodedCompressed
      val secpKeyFromCompressed = CryptoUtils.unsafeToSecpPublicKeyFromCompressed(compressedPub.toVector)
      val x = secpKeyFromCompressed.x
      val y = secpKeyFromCompressed.y
      val secpFromCoordinates = CryptoUtils.unsafeToSecpPublicKeyFromByteCoordinates(x, y)

      // we check we have the same key as the SDK
      compressedPub.toVector mustBe secpKeyFromCompressed.compressed
      // we compare the 2 ways to decode the key
      secpKeyFromCompressed.compressed.toVector mustBe secpFromCoordinates.compressed.toVector
    }
  }
}
