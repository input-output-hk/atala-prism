package io.iohk.atala.prism.node.crypto

import io.iohk.atala.prism.crypto.signature.ECSignature
import io.iohk.atala.prism.crypto.{EC, Sha256, Sha256Digest}
import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpECDSA, SecpPrivateKey, SecpPublicKey}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Try

// The end goal of this test suit is to be deleted
// Its purpose is to validate that the new implementation is
// equivalent to the old SDK it is replacing, Once the SDK is
// removed, then this tests should be deleted too
class CryptoTestsSpec extends AnyWordSpec {

  // public key encoding/decoding (compressed and uncompressed)
  // private key encoding/decoding
  // Signature validation

  "cryptoUtils library" should {

    // HASHING
    "Perform Sha256 hashing as the SDK" in {
      val msg = EC.INSTANCE.generateKeyPair().getPublicKey.getEncoded
      CryptoUtils.Sha256Hash.compute(msg).bytes mustBe Sha256.compute(msg).getValue.toVector
    }

    "encode hashes as the SDK" in {
      val msg = EC.INSTANCE.generateKeyPair().getPublicKey.getEncoded
      CryptoUtils.Sha256Hash.compute(msg).hexEncoded mustBe Sha256.compute(msg).getHexValue
    }

    "decode hex encoded hashes as the SDK" in {
      val hexEncoded = "c489e391b64dc18273047935edcb5e90d97da88b58ed9c2fa5e48dd3cf878f56"
      CryptoUtils.Sha256Hash.fromHex(hexEncoded).bytes mustBe Sha256Digest.fromHex(hexEncoded).getValue.toVector

      val invalidHex = "W489e391b64dc18273047935edcb5e90d97da88b58ed9c2fa5e48dd3cf878f56"
      val failed1 = Try(CryptoUtils.Sha256Hash.fromHex(invalidHex))
      val failed2 = Try(Sha256Digest.fromHex(invalidHex))
      (failed1.isFailure && failed2.isFailure) mustBe true
    }

    "decode hashes bytes as the SDK" in {
      val bytes = Array[Byte](-60, -119, -29, -111, -74, 77, -63, -126, 115, 4, 121, 53, -19, -53, 94, -112, -39, 125,
        -88, -117, 88, -19, -100, 47, -91, -28, -115, -45, -49, -121, -113, 86)
      CryptoUtils.Sha256Hash.fromBytes(bytes).bytes mustBe Sha256Digest.fromBytes(bytes).getValue.toVector

      val invalidBytes = Array[Byte]()
      val failed1 = Try(CryptoUtils.Sha256Hash.fromBytes(invalidBytes))
      val failed2 = Try(Sha256Digest.fromBytes(invalidBytes))
      (failed1.isFailure && failed2.isFailure) mustBe true
    }

    // SIGNING / VERIFICATION
    "can verify what SDK signs" in {
      val pair = EC.INSTANCE.generateKeyPair()
      val pub = pair.getPublicKey
      val priv = pair.getPrivateKey

      val msg = CryptoUtils.Sha256Hash.compute(pub.getEncodedCompressed).bytes.toArray
      val sig = EC.INSTANCE.signBytes(msg, priv)

      val secp = SecpPublicKey.unsafeToSecpPublicKeyFromCompressed(pub.getEncodedCompressed.toVector)
      SecpPublicKey.checkECDSASignature(msg, sig.getData, secp) mustBe true
    }

    "can verify what it signs" in {
      val pair = CryptoTestUtils.generateKeyPair()
      val pub = pair.publicKey
      val priv = pair.privateKey

      val msg = CryptoUtils.Sha256Hash.compute(pub.compressed).bytes.toArray

      val sig = SecpECDSA.signBytes(msg, priv).bytes
      SecpPublicKey.checkECDSASignature(msg, sig, pub) mustBe true
    }

    "can produce valid signatures according to SDK" in {
      val pair = CryptoTestUtils.generateKeyPair()
      val pub = CryptoTestUtils.getUnderlyingKey(pair.publicKey)
      val priv = pair.privateKey

      val msg = CryptoUtils.Sha256Hash.compute(pub.getEncodedCompressed).bytes.toArray
      val sig = new ECSignature(SecpECDSA.signBytes(msg, priv).bytes)

      EC.INSTANCE.verifyBytes(msg, pub, sig) mustBe true
    }

    // PUBLIC KEY ENCODING / DECODING
    "public key uncompressed encoding us compatible with SDK" in {
      val secpPublicKey = CryptoTestUtils.generateKeyPair().publicKey
      val sdkPubKey = EC.INSTANCE.generateKeyPair().getPublicKey

      val uSecp = secpPublicKey.unCompressed
      val uSdk = sdkPubKey.getEncoded

      // we parse the keys in the opposite library
      val parsedSDKKey = SecpPublicKey.unsafetoPublicKeyFromUncompressed(uSdk)
      val parsedSecpKey = EC.INSTANCE.toPublicKeyFromBytes(uSecp)

      // we compare the encodings
      uSdk.toVector mustBe parsedSDKKey.unCompressed.toVector
      uSecp.toVector mustBe parsedSecpKey.getEncoded.toVector
    }

    "private key encoding decoding" in {
      val pair = EC.INSTANCE.generateKeyPair()
      val privK = pair.getPrivateKey
      val encodedPvKey = privK.getEncoded
      val secp = SecpPrivateKey.unsafefromBytesCompressed(encodedPvKey)

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
