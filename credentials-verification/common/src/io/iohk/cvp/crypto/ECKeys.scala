package io.iohk.cvp.crypto

import java.io.IOException
import java.math.BigInteger
import java.security.{PublicKey, _}
import java.security.spec.{
  ECGenParameterSpec => JavaECGenParameterSpec,
  ECPoint => JavaECPoint,
  ECPrivateKeySpec => JavaECPrivateKeySpec,
  ECPublicKeySpec => JavaECPublicKeySpec
}

import org.bouncycastle.jcajce.provider.asymmetric.ec.{BCECPrivateKey, BCECPublicKey}
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.{ECNamedCurveSpec, ECPublicKeySpec => BCECPublicKeySpec}
import org.bouncycastle.math.ec.ECCurve

object ECKeys {

  java.security.Security.addProvider(new BouncyCastleProvider)

  val CURVE_NAME = "secp256k1"

  private val provider = "BC"
  private val keyFactory = KeyFactory.getInstance("EC", provider)
  private val ecParameterSpec = ECNamedCurveTable.getParameterSpec(CURVE_NAME)

  private val ecNamedCurveSpec = new ECNamedCurveSpec(
    ecParameterSpec.getName,
    ecParameterSpec.getCurve,
    ecParameterSpec.getG,
    ecParameterSpec.getN
  )

  /**
    * Generate P-256k/secp256k1/prime256v1 key pair
    */
  def generateKeyPair(): KeyPair = {
    val keyGen = KeyPairGenerator.getInstance("ECDSA", provider)
    val ecSpec = new JavaECGenParameterSpec(CURVE_NAME)
    keyGen.initialize(ecSpec, new SecureRandom())
    keyGen.generateKeyPair()
  }

  def toPrivateKey(d: Array[Byte]): PrivateKey = {
    toPrivateKey(toBigInt(d))
  }

  def toPrivateKey(d: BigInt): PrivateKey = {
    val spec = toPrivateKeySpec(d)
    keyFactory.generatePrivate(spec)
  }

  def toPublicKey(x: Array[Byte], y: Array[Byte]): PublicKey = {
    val spec = toPublicKeySpec(toBigInt(x), toBigInt(y))
    keyFactory.generatePublic(spec)
  }

  def toPublicKey(x: BigInt, y: BigInt): PublicKey = {
    val spec = toPublicKeySpec(x, y)
    keyFactory.generatePublic(spec)
  }

  def toPublicKey(d: Array[Byte]): PublicKey = {
    toPublicKey(toBigInt(d))
  }

  def toPublicKey(d: BigInt): PublicKey = {
    val Q = ecParameterSpec.getG.multiply(d.bigInteger)
    val pubSpec = new BCECPublicKeySpec(Q, ecParameterSpec)
    keyFactory.generatePublic(pubSpec)
  }

  def getECPoint(publicKey: PublicKey): JavaECPoint = {
    publicKey match {
      case k: BCECPublicKey => k.getW
    }
  }

  def getD(privateKey: PrivateKey): BigInt = {
    privateKey match {
      case k: BCECPrivateKey => k.getD
    }
  }

  def toEncodedPublicKey(curve: String, xBytes: Array[Byte], yBytes: Array[Byte]): EncodedPublicKey = {
    assert(curve == CURVE_NAME)
    val encodedBytes = encodePoint(xBytes, yBytes, ecParameterSpec.getCurve)
    EncodedPublicKey(encodedBytes.toVector)
  }

  def toEncodedPublicKey(publicKey: PublicKey): EncodedPublicKey = {
    val encodeBytes = encodePoint(getECPoint(publicKey), ecParameterSpec.getCurve)
    EncodedPublicKey(encodeBytes.toVector)
  }

  def toPublicKey(encodedPublicKey: EncodedPublicKey): PublicKey = {
    val ecPoint = toJavaECPoint(encodedPublicKey)
    toPublicKey(ecPoint.getAffineX, ecPoint.getAffineY)
  }

  /**
    * @param ecPoint points on elliptic curves
    * @param ecCurve an elliptic curve
    * @return Array[Byte]
    * @see sun.security.util.ECUtil#encodePoint()
    */
  private def encodePoint(ecPoint: JavaECPoint, ecCurve: ECCurve): Array[Byte] = {
    val xBytes = toUnsignedByteArray(ecPoint.getAffineX)
    val yBytes = toUnsignedByteArray(ecPoint.getAffineY)
    encodePoint(xBytes, yBytes, ecCurve)
  }

  /**
    * @param xBytes byte encoding of curve point x coordinate
    * @param yBytes byte encoding of curve point x coordinate
    * @param ecCurve an elliptic curve
    * @return Array[Byte]
    * @see sun.security.util.ECUtil#encodePoint()
    */
  private def encodePoint(xBytes: Array[Byte], yBytes: Array[Byte], ecCurve: ECCurve): Array[Byte] = {
    val size = ecCurve.getFieldSize + 7 >> 3
    if (xBytes.length <= size && yBytes.length <= size) {
      val arr = new Array[Byte](1 + (size << 1))
      arr(0) = 4 //Uncompressed point indicator for encoding
      Array.copy(xBytes, 0, arr, size - xBytes.length + 1, xBytes.length)
      Array.copy(yBytes, 0, arr, arr.length - yBytes.length, yBytes.length)
      arr
    } else throw new RuntimeException("Point coordinates do not match field size")
  }

  /**
    *
    * @param encodedPublicKey EncodedPublicKey is an uncompressed byte Array
    * @return ECPoint points on elliptic curve
    */
  private def toJavaECPoint(encodedPublicKey: EncodedPublicKey): JavaECPoint = {
    val ecCurve = ecParameterSpec.getCurve
    val bytes = encodedPublicKey.bytes.toArray
    if (bytes.nonEmpty && bytes(0) == 4) {
      val point = (bytes.length - 1) / 2
      if (point != ecCurve.getFieldSize + 7 >> 3) throw new IOException("Point does not match field size")
      else {
        val xArr: Array[Byte] = bytes.slice(1, 1 + point)
        val yArr: Array[Byte] = bytes.slice(point + 1, point + 1 + point)
        new JavaECPoint(new BigInteger(1, xArr), new BigInteger(1, yArr))
      }
    } else throw new IOException("Only uncompressed point format supported")
  }

  private def toUnsignedByteArray(src: BigInt): Array[Byte] = {
    src.toByteArray.dropWhile(_ == 0)
  }

  /**
    * It looks like the coordinates on secp256k1 are always possitive and keys are encoded without the byte sign
    * that Java uses for encoding/decoding a big integer.
    */
  private def toBigInt(bytes: Array[Byte]): BigInt = {
    BigInt(1, bytes)
  }

  private def toPrivateKeySpec(d: BigInt): JavaECPrivateKeySpec = {
    new JavaECPrivateKeySpec(d.bigInteger, ecNamedCurveSpec)
  }

  private def toPublicKeySpec(x: BigInt, y: BigInt): JavaECPublicKeySpec = {
    val ecPoint = new JavaECPoint(x.bigInteger, y.bigInteger)

    new JavaECPublicKeySpec(ecPoint, ecNamedCurveSpec)
  }

  case class EncodedPublicKey(bytes: Vector[Byte]) extends AnyVal
}
