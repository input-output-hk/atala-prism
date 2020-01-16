package io.iohk.cvp.crypto

import java.security._
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

  def toEncodePublicKey(x: BigInt, y: BigInt): Array[Byte] = {
    encodePoint(getECPoint(toPublicKey(x, y)), ecParameterSpec.getCurve)
  }

  /**
    * @param ecPoint
    * @param ecCurve
    * @return Array[Byte]
    *
    */
  def encodePoint(ecPoint: JavaECPoint, ecCurve: ECCurve): Array[Byte] = {
    val size = ecCurve.getFieldSize + 7 >> 3
    val xArr = toUnsignedByteArray(ecPoint.getAffineX)
    val yArr = toUnsignedByteArray(ecPoint.getAffineY)
    if (xArr.length <= size && yArr.length <= size) {
      val arr = new Array[Byte](1 + (size << 1))
      arr(0) = 4 //Uncompressed point indicator for encoding
      Array.copy(xArr, 0, arr, size - xArr.length + 1, xArr.length)
      Array.copy(yArr, 0, arr, arr.length - yArr.length, yArr.length)
      arr
    } else throw new RuntimeException("Point coordinates do not match field size")
  }

  private def toUnsignedByteArray(src: BigInt): Array[Byte] = {
    val asByteArray = src.toByteArray
    if (asByteArray.head == 0) asByteArray.tail
    else asByteArray
  }

  /**
    * It looks like the coordinates on secp256k1 are always possitive and keys are encoded without the byte sign
    * that Java uses for encoding/decoding a big integer.
    */
  private def toBigInt(bytes: Array[Byte]) = {
    BigInt(1, bytes)
  }

  private def toPrivateKeySpec(d: BigInt): JavaECPrivateKeySpec = {
    new JavaECPrivateKeySpec(d.bigInteger, ecNamedCurveSpec)
  }

  private def toPublicKeySpec(x: BigInt, y: BigInt): JavaECPublicKeySpec = {
    val ecPoint = new JavaECPoint(x.bigInteger, y.bigInteger)

    new JavaECPublicKeySpec(ecPoint, ecNamedCurveSpec)
  }

}
