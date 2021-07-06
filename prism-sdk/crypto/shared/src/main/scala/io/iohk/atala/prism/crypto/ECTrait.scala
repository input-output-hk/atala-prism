package io.iohk.atala.prism.crypto

import io.iohk.atala.prism.crypto.ECConfig.{p, b}
import io.iohk.atala.prism.util.BigIntOps.toBigInt

import scala.util.Try

/**
  * Trait that implements all shared behavior between js/EC and jvm/EC.
  *
  * <p>Client code should use `EC` without thinking on which implementation (JS, JVM) is being used. This will allow
  * downstream cross-compiled projects to work seamlessly.
  */
trait ECTrait {

  /**
    * Generates a P-256k/secp256k1/prime256v1 key-pair.
    */
  def generateKeyPair(): ECKeyPair

  /**
    * Returns the key-pair represented by the given private key's `D` as byte array.
    */
  def toKeyPairFromPrivateKey(d: Array[Byte]): ECKeyPair = {
    toKeyPairFromPrivateKey(toBigInt(d))
  }

  /**
    * Returns the key-pair represented by the given private key's `D` as number.
    */
  def toKeyPairFromPrivateKey(d: BigInt): ECKeyPair = {
    ECKeyPair(toPrivateKey(d), toPublicKeyFromPrivateKey(d))
  }

  /**
    * Returns the private key represented by the given byte array.
    */
  def toPrivateKey(d: Array[Byte]): ECPrivateKey = {
    toPrivateKey(toBigInt(d))
  }

  /**
    * Returns the private key represented by the given number.
    */
  def toPrivateKey(d: BigInt): ECPrivateKey

  /**
    * Returns the public key represented by the given encoded byte array.
    */
  def toPublicKey(encoded: Array[Byte]): ECPublicKey = {
    val expectedLength = 1 + 2 * ECConfig.CURVE_FIELD_BYTE_SIZE
    require(
      encoded.length == expectedLength,
      s"Encoded byte array's expected length is $expectedLength, but got ${encoded.length}"
    )
    require(encoded(0) == 4, s"First byte was expected to be 4, but got ${encoded(0)}")

    val xBytes = java.util.Arrays.copyOfRange(encoded, 1, 1 + ECConfig.CURVE_FIELD_BYTE_SIZE)
    val yBytes = java.util.Arrays.copyOfRange(encoded, 1 + ECConfig.CURVE_FIELD_BYTE_SIZE, encoded.length)
    toPublicKey(xBytes, yBytes)
  }

  /**
    * Returns the public key represented by the given coordinates as byte arrays.
    */
  def toPublicKey(x: Array[Byte], y: Array[Byte]): ECPublicKey = {
    toPublicKey(toBigInt(x), toBigInt(y))
  }

  /**
    * Returns the public key represented by the given coordinates.
    */
  def toPublicKey(x: BigInt, y: BigInt): ECPublicKey

  /**
    * Returns the public key uncompressed from the compressed representation.
    */
  def toPublicKeyFromCompressed(encoded: Array[Byte]): Try[ECPublicKey]

  /**
    * Returns the public key represented by the given private key's `D` as byte array.
    */
  def toPublicKeyFromPrivateKey(d: Array[Byte]): ECPublicKey = {
    toPublicKeyFromPrivateKey(toBigInt(d))
  }

  /**
    * Returns the public key represented by the given private key's `D` as number.
    */
  def toPublicKeyFromPrivateKey(d: BigInt): ECPublicKey

  /**
    * Signs the given text with the given private key.
    */
  def sign(text: String, privateKey: ECPrivateKey): ECSignature = {
    sign(text.getBytes("UTF-8"), privateKey)
  }

  /**
    * Signs the given data with the given private key.
    */
  def sign(data: Array[Byte], privateKey: ECPrivateKey): ECSignature

  /**
    * Verifies whether the given text matches the given signature with the given public key.
    */
  def verify(text: String, publicKey: ECPublicKey, signature: ECSignature): Boolean = {
    verify(text.getBytes("UTF-8"), publicKey, signature)
  }

  /**
    * Verifies whether the given data matches the given signature with the given public key.
    */
  def verify(data: Array[Byte], publicKey: ECPublicKey, signature: ECSignature): Boolean

  /**
    * Verifies whether the given data matches the given signature with the given public key.
    */
  def isSecp256k1(point: ECPoint): Boolean = {
    val right = point.x * point.x * point.x + b
    val left = point.y * point.y
    // see https://en.bitcoin.it/wiki/Secp256k1
    ((left - right) mod p) == 0
  }
}
