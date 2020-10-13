package io.iohk.atala.crypto

import io.iohk.atala.crypto.ECUtils.hexToBytes
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

/**
  * Base class to be extended by tests for {@link ECTrait} implementations.
  * @param ec the implementation under test
  */
abstract class ECSpecBase(val ec: ECTrait) extends AnyWordSpec {
  // Test bytes generated randomly with `Array.fill(20)((scala.util.Random.nextInt(256) - 128).toByte)`
  private val TEST_BYTES =
    Array[Byte](-107, 101, 68, 118, 27, 74, 29, 50, -32, 72, 47, -127, -49, 3, -8, -55, -63, -66, 46, 125)

  "EC" should {
    "generate a key pair" in {
      val keyPair = ec.generateKeyPair()

      keyPair.privateKey.getEncoded.length mustBe >(0)
      keyPair.privateKey.getHexEncoded.length mustBe 2 * keyPair.privateKey.getEncoded.length
      keyPair.publicKey.getEncoded.length mustBe (ECConfig.CURVE_FIELD_BYTE_SIZE * 2 + 1)
      keyPair.publicKey.getHexEncoded.length mustBe 2 * keyPair.publicKey.getEncoded.length
    }

    "generate the private key from the encoded byte array" in {
      val keyPair = ec.generateKeyPair()
      val encodedPrivateKey = keyPair.privateKey.getEncoded

      ec.toPrivateKey(encodedPrivateKey) mustBe keyPair.privateKey
      ec.toPrivateKey(BigInt(1, encodedPrivateKey)) mustBe keyPair.privateKey
    }

    "generate the public key from the curve point" in {
      val keyPair = ec.generateKeyPair()
      val ecPoint = keyPair.publicKey.getCurvePoint

      ec.toPublicKey(ecPoint.x, ecPoint.y) mustBe keyPair.publicKey
      ec.toPublicKey(ecPoint.x.toByteArray, ecPoint.y.toByteArray) mustBe keyPair.publicKey
    }

    "generate the public key from the encoded byte array" in {
      val keyPair = ec.generateKeyPair()
      val encodedPublicKey = keyPair.publicKey.getEncoded

      ec.toPublicKey(encodedPublicKey) mustBe keyPair.publicKey
    }

    "generate the public key from private key" in {
      val keyPair = ec.generateKeyPair()
      val encodedPrivateKey = keyPair.privateKey.getEncoded

      ec.toPublicKeyFromPrivateKey(encodedPrivateKey) mustBe keyPair.publicKey
      ec.toPublicKeyFromPrivateKey(BigInt(1, encodedPrivateKey)) mustBe keyPair.publicKey
    }

    "generate the same private key across all implementations" in {
      val hexEncodedPrivateKey = "933c25b9e0b10b0618517edeb389b1b5ba5e781f377af6f573a1af354d008034"

      val privateKey = ec.toPrivateKey(hexToBytes(hexEncodedPrivateKey))

      privateKey.getHexEncoded mustBe hexEncodedPrivateKey
    }

    "generate the same public key across all implementations" in {
      val hexEncodedPublicKey =
        "0477d650217424671208f06ed816dab6c09e6b08c4da0f2f46ead049dd5fbd1c82cd23343346003d4c7faf24ed6314bf340e7882941fd69929526cc889a0f93a1c"

      val publicKey = ec.toPublicKey(hexToBytes(hexEncodedPublicKey))

      publicKey.getHexEncoded mustBe hexEncodedPublicKey
    }

    "sign and verify a text" in {
      val keyPair = ec.generateKeyPair()
      val text = "The quick brown fox jumps over the lazy dog"

      val signature = ec.sign(text, keyPair.privateKey)

      ec.verify(text, keyPair.publicKey, signature) mustBe true
    }

    "sign and verify data" in {
      val keyPair = ec.generateKeyPair()
      val data = TEST_BYTES

      val signature = ec.sign(data, keyPair.privateKey)

      ec.verify(data, keyPair.publicKey, signature) mustBe true
    }

    "not verify the wrong input" in {
      val keyPair = ec.generateKeyPair()
      val wrongKeyPair = ec.generateKeyPair()
      val text = "The quick brown fox jumps over the lazy dog"
      val wrongText = "Wrong text"

      val signature = ec.sign(text, keyPair.privateKey)
      val wrongSignature = ec.sign(wrongText, keyPair.privateKey)

      ec.verify(wrongText, keyPair.publicKey, signature) mustBe false
      ec.verify(text, wrongKeyPair.publicKey, signature) mustBe false
      ec.verify(text, keyPair.publicKey, wrongSignature) mustBe false
    }

    "verify the same signature in all implementations" in {
      val hexEncodedPrivateKey = "0123fbf1050c3fc060b709fdcf240e766a41190c40afc5ac7a702961df8313c0"
      val hexEncodedSignature =
        "304602210090bb8c027742a21eeef6687acb5378691cae78a190d3e9a833904988f0df8172022100b6d060f89d26c862c462746afdb812980b61c3a59cb2f61f6ff65d2e8f702d23"
      val privateKey = ec.toPrivateKey(hexToBytes(hexEncodedPrivateKey))
      val data = TEST_BYTES
      val signature = ECSignature(hexToBytes(hexEncodedSignature))

      ec.verify(data, ec.toPublicKeyFromPrivateKey(privateKey.getD), signature) mustBe true
    }
  }
}
