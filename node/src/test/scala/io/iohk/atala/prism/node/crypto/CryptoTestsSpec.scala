package io.iohk.atala.prism.node.crypto

import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpPrivateKey, SecpPublicKey}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec

class CryptoTestsSpec extends AnyWordSpec {

  "crypto library" should {
    "public key uncompressed encoding decoding" in {
      ???
    }

    "private key encoding decoding" in {
      val pair = EC.INSTANCE.generateKeyPair()
      val privK = pair.getPrivateKey
      val encodedPvKey = privK.getEncoded
      val secp = SecpPrivateKey.unsafefromBytesCompressed(encodedPvKey)

      println(secp.bytes.toVector)
      println(secp.getEncoded.toVector)

      encodedPvKey.toVector mustBe secp.getEncoded.toVector
    }

    "encoded uncompressed should be the same as SDK" in {
      val pair = EC.INSTANCE.generateKeyPair()
      val compressedPub = pair.getPublicKey.getEncodedCompressed
      val secpKeyFromCompressed = SecpPublicKey.unsafeToSecpPublicKeyFromCompressed(compressedPub.toVector)
      pair.getPublicKey.getEncoded.toVector mustBe secpKeyFromCompressed.unCompressed.toVector
    }

    "Must generate the same key from different encodings" in {
      val pair = EC.INSTANCE.generateKeyPair()
      val compressedPub = pair.getPublicKey.getEncodedCompressed
      val secpKeyFromCompressed = SecpPublicKey.unsafeToSecpPublicKeyFromCompressed(compressedPub.toVector)
      val x = secpKeyFromCompressed.x
      val y = secpKeyFromCompressed.y
      val secpFromCoordinates = SecpPublicKey.unsafeToSecpPublicKeyFromByteCoordinates(x, y)

      // we check we have the same key as the SDK
      compressedPub.toVector mustBe secpKeyFromCompressed.compressed
      // we compare the 2 ways to decode the key
      secpKeyFromCompressed.compressed.toVector mustBe secpFromCoordinates.compressed.toVector
    }
  }
}
