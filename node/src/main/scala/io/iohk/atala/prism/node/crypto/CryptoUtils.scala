package io.iohk.atala.prism.node.crypto

import io.iohk.atala.prism.node.models.ProtocolConstants
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util
import org.bouncycastle.jce.interfaces.ECPublicKey

import java.security.{KeyFactory, MessageDigest, PublicKey, Security, Signature}
import org.bouncycastle.jce.{ECNamedCurveTable, ECPointUtil}
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.security.spec.{ECPoint, ECPublicKeySpec}

object CryptoUtils {
  trait SecpPublicKey {
    private[crypto] def publicKey: PublicKey
    def curveName: String = ProtocolConstants.secpCurveName
    def compressed: Array[Byte] = publicKey
      .asInstanceOf[ECPublicKey]
      .getQ
      .getEncoded(true)
    def x: Array[Byte] = publicKey.asInstanceOf[ECPublicKey].getQ.getAffineXCoord.getEncoded
    def y: Array[Byte] = publicKey.asInstanceOf[ECPublicKey].getQ.getAffineYCoord.getEncoded
  }

  // We define the constructor to SecpKeys private so that the only way to generate
  // these keys is by using the methods unsafeToPublicKeyFromByteCoordinates and
  // unsafeToPublicKeyFromCompressed.
  private object SecpPublicKey {
    private class SecpPublicKeyImpl(pubKey: PublicKey) extends SecpPublicKey {
      override private[crypto] def publicKey: PublicKey = pubKey
    }

    def fromPublicKey(key: PublicKey): SecpPublicKey = new SecpPublicKeyImpl(key)
  }

  private val provider = new BouncyCastleProvider()
  private val PUBLIC_KEY_COORDINATE_BYTE_SIZE: Int = 32

  Security.addProvider(provider)

  def hash(bArray: Array[Byte]): Vector[Byte] = {
    MessageDigest
      .getInstance("SHA-256")
      .digest(bArray)
      .toVector
  }

  def bytesToHex(bytes: Vector[Byte]): String = {
    bytes.map(byte => f"${byte & 0xff}%02x").mkString
  }

  def hexedHash(bArray: Array[Byte]): String = {
    bytesToHex(hash(bArray))
  }

  def checkECDSASignature(msg: Array[Byte], sig: Array[Byte], pubKey: SecpPublicKey): Boolean = {
    val ecdsaVerify = Signature.getInstance("SHA256withECDSA", provider)
    ecdsaVerify.initVerify(pubKey.publicKey)
    ecdsaVerify.update(msg.toArray)
    ecdsaVerify.verify(sig.toArray)
  }

  def unsafeToSecpPublicKeyFromByteCoordinates(x: Array[Byte], y: Array[Byte]): SecpPublicKey = {
    def trimLeadingZeroes(arr: Array[Byte], c: String): Array[Byte] = {
      val trimmed = arr.dropWhile(_ == 0.toByte)
      require(
        trimmed.length <= PUBLIC_KEY_COORDINATE_BYTE_SIZE,
        s"Expected $c coordinate byte length to be less than or equal ${PUBLIC_KEY_COORDINATE_BYTE_SIZE}, but got ${trimmed.length} bytes"
      )
      trimmed
    }

    val xTrimmed = trimLeadingZeroes(x, "x")
    val yTrimmed = trimLeadingZeroes(y, "y")
    val xInteger = BigInt(1, xTrimmed)
    val yInteger = BigInt(1, yTrimmed)
    unsafeToSecpPublicKeyFromBigIntegerCoordinates(xInteger, yInteger)
  }

  private def unsafeToSecpPublicKeyFromBigIntegerCoordinates(x: BigInt, y: BigInt): SecpPublicKey = {
    val params = ECNamedCurveTable.getParameterSpec("secp256k1")
    val fact = KeyFactory.getInstance("ECDSA", provider)
    val curve = params.getCurve
    val ellipticCurve = EC5Util.convertCurve(curve, params.getSeed)
    val point = new ECPoint(x.bigInteger, y.bigInteger)
    val params2 = EC5Util.convertSpec(ellipticCurve, params)
    val keySpec = new ECPublicKeySpec(point, params2)
    SecpPublicKey.fromPublicKey(fact.generatePublic(keySpec))
  }

  def unsafeToSecpPublicKeyFromCompressed(com: Vector[Byte]): SecpPublicKey = {
    val params = ECNamedCurveTable.getParameterSpec("secp256k1")
    val fact = KeyFactory.getInstance("ECDSA", provider)
    val curve = params.getCurve
    val ellipticCurve = EC5Util.convertCurve(curve, params.getSeed)
    val point = ECPointUtil.decodePoint(ellipticCurve, com.toArray)
    val params2 = EC5Util.convertSpec(ellipticCurve, params)
    val keySpec = new ECPublicKeySpec(point, params2)
    SecpPublicKey.fromPublicKey(fact.generatePublic(keySpec))
  }
}
