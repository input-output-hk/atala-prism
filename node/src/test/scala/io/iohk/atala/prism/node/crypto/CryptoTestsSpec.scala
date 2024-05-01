package io.iohk.atala.prism.node.crypto

import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpECDSA, SecpPrivateKey, SecpPublicKey}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Try

class CryptoTestsSpec extends AnyWordSpec {

  "cryptoUtils library" should {

    // HASHING
    "Perform Sha256 hashing as expected" in {
      val msg = Array[Byte](4, -39, 51, 30, -58, -105, -62, -88, 76, -84, 31, -44, 56, 8, -101, 78, -26, 36, -77, 65,
        -3, -60, -45, -30, -16, -124, -70, 39, 113, 116, 27, 30, -37, 18, -3, 70, 28, -73, -62, 59, -109, -72, -3, -60,
        -14, 100, -77, 2, -18, -80, -36, 95, 108, 116, 14, 99, 71, 112, -2, 17, 80, -82, -86, 1, 109)
      val hash = Vector(-93, 72, -18, 74, -8, -101, 74, -54, 72, 76, -19, 36, 61, -74, -22, 32, -57, 81, 52, 110, -92,
        -48, 91, 36, -68, -30, -46, 96, 27, 91, -112, -124)
      CryptoUtils.Sha256Hash.compute(msg).bytes mustBe hash
    }

    "encode hashes as expected" in {
      val msg = Array[Byte](4, 63, -31, 28, -126, -125, -126, 15, -106, -9, 75, -31, -4, -128, 47, 40, -53, -12, 106,
        -32, -36, 124, 77, -53, 28, -45, 7, -85, 38, 27, -42, 113, 65, -18, -54, 31, 61, -33, -88, -103, -128, 64, -20,
        112, 10, 107, -60, 54, 3, 16, -123, -18, -39, -107, 111, 35, -125, 69, 15, -127, 38, 3, -70, -27, 5)
      val encodedHash = "1eaec245dacb5aef8abe3bf1a5bea2ce78738d311efbdabafaa52db7f03fc7f1"

      CryptoUtils.Sha256Hash.compute(msg).hexEncoded mustBe encodedHash
    }

    "decode hex encoded hashes as expected" in {
      val hexEncoded = "c489e391b64dc18273047935edcb5e90d97da88b58ed9c2fa5e48dd3cf878f56"
      val decodedHash = Vector(-60, -119, -29, -111, -74, 77, -63, -126, 115, 4, 121, 53, -19, -53, 94, -112, -39, 125,
        -88, -117, 88, -19, -100, 47, -91, -28, -115, -45, -49, -121, -113, 86)

      CryptoUtils.Sha256Hash.fromHex(hexEncoded).bytes mustBe decodedHash

      val invalidHex = "W489e391b64dc18273047935edcb5e90d97da88b58ed9c2fa5e48dd3cf878f56"
      val failed = Try(CryptoUtils.Sha256Hash.fromHex(invalidHex))
      failed.isFailure mustBe true
    }

    "decode hashes bytes as expected" in {
      val bytes = Array[Byte](-60, -119, -29, -111, -74, 77, -63, -126, 115, 4, 121, 53, -19, -53, 94, -112, -39, 125,
        -88, -117, 88, -19, -100, 47, -91, -28, -115, -45, -49, -121, -113, 86)

      CryptoUtils.Sha256Hash.fromBytes(bytes).bytes mustBe bytes.toVector

      val invalidBytes = Array[Byte]()
      val failed = Try(CryptoUtils.Sha256Hash.fromBytes(invalidBytes))
      failed.isFailure mustBe true
    }

    // SIGNING / VERIFICATION
    "can verify a valid signature from another library" in {
      val compressedPublicKey = Vector[Byte](3, -121, 104, 43, 98, 19, 38, 66, -113, -33, -73, -104, -36, -63, 14, 49,
        -45, 69, 36, -5, -20, -16, 67, -57, -104, -39, -1, 81, -36, -28, -113, 124, 30)
      val msg = Array[Byte](39, -13, -109, 70, 109, -44, -18, 8, -31, 21, -40, -68, 101, 65, 39, 30, -120, 9, -119, 53,
        -80, 116, 83, 46, -81, 49, 109, -98, -88, -38, 46, 105)
      val sig = Array[Byte](48, 69, 2, 33, 0, -48, 95, 112, 13, 69, -73, -73, 50, 45, 95, -88, 90, -128, -3, -115, 66,
        -91, 126, 108, -61, -47, 19, 74, -98, 58, -42, 36, 8, 45, 2, 93, -102, 2, 32, 27, 91, -1, 57, -85, 60, -73, 85,
        -51, -54, -89, 30, -89, 27, -42, -10, 74, -65, 46, -77, -23, 20, 126, 16, -58, -21, 56, 36, 127, -65, -10, -62)

      val secp = SecpPublicKey.unsafeToSecpPublicKeyFromCompressed(compressedPublicKey)
      SecpPublicKey.checkECDSASignature(msg, sig, secp) mustBe true
    }

    "can verify what it signs" in {
      val pair = CryptoTestUtils.generateKeyPair()
      val pub = pair.publicKey
      val priv = pair.privateKey

      val msg = CryptoUtils.Sha256Hash.compute(pub.compressed).bytes.toArray

      val sig = SecpECDSA.signBytes(msg, priv).bytes
      SecpPublicKey.checkECDSASignature(msg, sig, pub) mustBe true
    }

    // PUBLIC KEY ENCODING / DECODING
    "public key uncompressed encoding / decoding works as expected" in {
      val originalUncompressed = CryptoTestUtils.generateKeyPair().publicKey.unCompressed
      val secpPublicKey = SecpPublicKey.unsafetoPublicKeyFromUncompressed(originalUncompressed)

      originalUncompressed.toVector mustBe secpPublicKey.unCompressed.toVector

      // we test the same with an externally generated key
      val externalUncompressedKey = Array[Byte](4, -118, 38, -87, -93, -27, -9, 88, 11, 37, -118, 49, 87, 93, -101, 22,
        10, -51, -116, -89, 26, -58, -15, -21, 116, 82, 30, -13, 29, -75, 52, 2, -106, -83, 108, -58, 49, 30, 11, 43,
        -8, -61, 78, 1, 16, 70, 102, 101, -74, 120, 19, -41, 46, 45, -29, -82, -53, 97, 55, 79, 68, -16, -73, -82, -7)
      val parsedKey = SecpPublicKey.unsafetoPublicKeyFromUncompressed(externalUncompressedKey)

      externalUncompressedKey.toVector mustBe parsedKey.unCompressed.toVector
    }

    "public key compressed encoding / decoding works as expected" in {
      val originalCompressed = CryptoTestUtils.generateKeyPair().publicKey.compressed
      val secpPublicKey = SecpPublicKey.unsafeToSecpPublicKeyFromCompressed(originalCompressed.toVector)

      originalCompressed.toVector mustBe secpPublicKey.compressed.toVector

      // we test the same with an externally generated key
      val externalCompressedKey = Vector[Byte](3, -7, -57, -39, 107, 103, -70, -127, -67, 43, 41, 75, -74, 99, -122,
        -34, -67, -19, 10, -25, 36, -52, 47, 89, -52, 27, 3, 10, 27, 0, 3, 102, 122)
      val parsedKey = SecpPublicKey.unsafeToSecpPublicKeyFromCompressed(externalCompressedKey)

      externalCompressedKey mustBe parsedKey.compressed.toVector
    }

    "public key coordinates encoding / decoding works as expected" in {
      val originalSecpPublicKey = CryptoTestUtils.generateKeyPair().publicKey
      val originalX = originalSecpPublicKey.x
      val originalY = originalSecpPublicKey.y
      val secpPublicKey = SecpPublicKey.unsafeToSecpPublicKeyFromByteCoordinates(originalX, originalY)

      originalX.toVector mustBe secpPublicKey.x.toVector
      originalY.toVector mustBe secpPublicKey.y.toVector

      // we test the same with externally generated coordinates
      val externalX = Array[Byte](-23, -72, 42, -33, -57, -68, 124, -72, -7, -9, 72, -78, 87, -75, 118, 50, -64, 104,
        69, 31, -114, 5, 81, 83, 77, -70, 116, 45, -12, -123, 49, 72)
      val externalY = Array[Byte](-92, 7, 60, 55, -100, -53, -96, -41, 112, -40, 50, 6, 93, -87, -58, -9, -5, -86, -20,
        94, -97, 106, -74, -53, 118, -37, 79, 119, -106, -21, -111, 45)
      val parsedKey = SecpPublicKey.unsafeToSecpPublicKeyFromByteCoordinates(externalX, externalY)

      externalX.toVector mustBe parsedKey.x.toVector
      externalY.toVector mustBe parsedKey.y.toVector
    }

    "Must generate the same key from different encodings" in {
      val publicKey = CryptoTestUtils.generateKeyPair().publicKey
      val compressed = publicKey.compressed
      val unCompressed = publicKey.unCompressed
      val x = publicKey.x
      val y = publicKey.y
      val secpKeyFromCompressed = SecpPublicKey.unsafeToSecpPublicKeyFromCompressed(compressed.toVector)
      val secpKeyFromUncompressed = SecpPublicKey.unsafetoPublicKeyFromUncompressed(unCompressed)
      val secpFromCoordinates = SecpPublicKey.unsafeToSecpPublicKeyFromByteCoordinates(x, y)

      compressed.toVector mustBe secpKeyFromCompressed.compressed.toVector
      compressed.toVector mustBe secpKeyFromUncompressed.compressed.toVector
      compressed.toVector mustBe secpFromCoordinates.compressed.toVector
    }

    "private key encoding / decoding works as expected" in {
      val originalEncodedKey = CryptoTestUtils.generateKeyPair().privateKey.getEncoded
      val secpPrivateKey = SecpPrivateKey.unsafefromBytesCompressed(originalEncodedKey)

      originalEncodedKey.toVector mustBe secpPrivateKey.getEncoded.toVector

      // we test the same with an externally generated key
      val externalEncodedKey = Array[Byte](27, 117, -8, -43, -76, 44, -64, -93, -97, 29, -106, 88, 43, 76, 1, -48, 114,
        -11, -4, 47, -100, -127, 31, -112, -51, -41, 102, -45, -64, 85, -126, -77)
      val parsedSDKKey = SecpPrivateKey.unsafefromBytesCompressed(externalEncodedKey)

      externalEncodedKey.toVector mustBe parsedSDKKey.getEncoded.toVector
    }
  }
}
