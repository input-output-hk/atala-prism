package io.iohk.atala.prism.node.crypto

import com.google.protobuf.ByteString
import io.iohk.atala.prism.node.models.ProtocolConstants
import io.iohk.atala.prism.protos.node_models.CompressedECKeyData
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util
import org.bouncycastle.jce.interfaces.{ECPrivateKey, ECPublicKey}
import java.security.{KeyFactory, MessageDigest, PrivateKey, PublicKey, Security, Signature}
import org.bouncycastle.jce.{ECNamedCurveTable, ECPointUtil}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import java.math.BigInteger
import java.security.spec.{ECParameterSpec, ECPoint, ECPrivateKeySpec, ECPublicKeySpec}
import scala.util.Try

object CryptoUtils {

  private val provider = new BouncyCastleProvider()
  Security.addProvider(provider)

  implicit class SecpPublicKeyOps(pubKey: SecpPublicKey) {
    def toProto: CompressedECKeyData =
      CompressedECKeyData(curve = ProtocolConstants.secpCurveName, data = ByteString.copyFrom(pubKey.compressed))
  }

  sealed trait SecpPublicKey {
    private[crypto] def publicKey: PublicKey
    def curveName: String = ProtocolConstants.secpCurveName
    def compressed: Array[Byte] = publicKey
      .asInstanceOf[ECPublicKey]
      .getQ
      .getEncoded(true)
    def x: Array[Byte] = publicKey.asInstanceOf[ECPublicKey].getQ.getAffineXCoord.getEncoded
    def y: Array[Byte] = publicKey.asInstanceOf[ECPublicKey].getQ.getAffineYCoord.getEncoded
    def unCompressed: Array[Byte] = {
      (List[Byte](4) ++ x.toList ++ y.toList).toArray
    }
  }

  private[crypto] class SecpPublicKeyImpl(pubKey: PublicKey) extends SecpPublicKey {
    override private[crypto] def publicKey: PublicKey = pubKey
  }

  object SecpPublicKey {

    private[crypto] def fromPublicKey(key: PublicKey): SecpPublicKey = new SecpPublicKeyImpl(key)

    def unsafeFromCompressed(com: Vector[Byte]): SecpPublicKey = {
      val params = ECNamedCurveTable.getParameterSpec("secp256k1")
      val fact = KeyFactory.getInstance("ECDSA", provider)
      val curve = params.getCurve
      val ellipticCurve = EC5Util.convertCurve(curve, params.getSeed)
      val point = ECPointUtil.decodePoint(ellipticCurve, com.toArray)
      val params2 = EC5Util.convertSpec(ellipticCurve, params)
      val keySpec = new ECPublicKeySpec(point, params2)
      SecpPublicKey.fromPublicKey(fact.generatePublic(keySpec))
    }

    def unsafeFromByteCoordinates(x: Array[Byte], y: Array[Byte]): SecpPublicKey = {
      val PUBLIC_KEY_COORDINATE_BYTE_SIZE: Int = 32
      def trimLeadingZeroes(arr: Array[Byte], c: String): Array[Byte] = {
        val trimmed = arr.dropWhile(_ == 0.toByte)
        require(
          trimmed.length <= PUBLIC_KEY_COORDINATE_BYTE_SIZE,
          s"Expected $c coordinate byte length to be less than or equal $PUBLIC_KEY_COORDINATE_BYTE_SIZE, but got ${trimmed.length} bytes"
        )
        trimmed
      }

      val xTrimmed = trimLeadingZeroes(x, "x")
      val yTrimmed = trimLeadingZeroes(y, "y")
      val xInteger = BigInt(1, xTrimmed)
      val yInteger = BigInt(1, yTrimmed)
      SecpPublicKey.unsafeFromBigIntegerCoordinates(xInteger, yInteger)
    }

    private def unsafeFromBigIntegerCoordinates(x: BigInt, y: BigInt): SecpPublicKey = {
      val params = ECNamedCurveTable.getParameterSpec("secp256k1")
      val fact = KeyFactory.getInstance("ECDSA", provider)
      val curve = params.getCurve
      val ellipticCurve = EC5Util.convertCurve(curve, params.getSeed)
      val point = new ECPoint(x.bigInteger, y.bigInteger)
      val params2 = EC5Util.convertSpec(ellipticCurve, params)
      val keySpec = new ECPublicKeySpec(point, params2)
      SecpPublicKey.fromPublicKey(fact.generatePublic(keySpec))
    }

    def unsafeFromUncompressed(bytes: Array[Byte]): SecpPublicKey = {
      val PRIVATE_KEY_BYTE_SIZE: Int = 32
      val pointSize = PRIVATE_KEY_BYTE_SIZE * 2 + 1
      require(bytes.length == pointSize, s"Invalid public key bytes length, ${bytes.length}")

      val uncompressedPrefix = bytes.head
      require(uncompressedPrefix == 0x04.toByte, "Invalid uncompressed public key prefix")

      val xBytes = bytes.slice(1, pointSize / 2 + 1)
      val yBytes = bytes.slice(pointSize / 2 + 1, pointSize + 1)

      val x = new BigInteger(1, xBytes)
      val y = new BigInteger(1, yBytes)
      unsafeFromBigIntegerCoordinates(x, y)
    }

    def isSecp256k1(key: SecpPublicKey): Boolean = {
      val params = ECNamedCurveTable.getParameterSpec("secp256k1")
      val curve = params.getCurve
      val x = new BigInteger(1, key.x)
      val y = new BigInteger(1, key.y)
      Try(curve.validatePoint(x, y)).isSuccess
    }
  }

  sealed trait SecpECDSASignature {
    def bytes: Array[Byte]
  }

  private[crypto] case class SecpECDSASignatureImpl(bytes: Array[Byte]) extends SecpECDSASignature

  object SecpECDSASignature {
    def fromBytes(bytes: Array[Byte]): SecpECDSASignature = SecpECDSASignatureImpl(bytes)
  }

  sealed trait SecpPrivateKey {
    private[crypto] def bytes: Array[Byte]
    private[crypto] def privateKey: PrivateKey
    def getEncoded: Array[Byte]
  }

  private[crypto] case class SecpPrivateKeyImpl(bytes: Array[Byte]) extends SecpPrivateKey {
    override def getEncoded: Array[Byte] = {
      privateKey
        .asInstanceOf[ECPrivateKey]
        .getD
        .toByteArray
        .dropWhile(_ == 0)
    }

    override def privateKey: PrivateKey = {
      val ecParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
      val ecNamedCurveSpec: ECParameterSpec = new ECNamedCurveSpec(
        ecParameterSpec.getName,
        ecParameterSpec.getCurve,
        ecParameterSpec.getG,
        ecParameterSpec.getN
      )
      val bigInt = new BigInteger(1, bytes)
      val spec = new ECPrivateKeySpec(bigInt, ecNamedCurveSpec)
      val keyFactory = KeyFactory.getInstance("EC", provider)
      keyFactory.generatePrivate(spec)
    }
  }

  object SecpPrivateKey {
    def unsafeFromBytesCompressed(bytes: Array[Byte]): SecpPrivateKey = SecpPrivateKeyImpl(bytes)
  }

  object SecpECDSA {
    def signBytes(msg: Array[Byte], privateKey: SecpPrivateKey): SecpECDSASignature = {
      val signer = Signature.getInstance("SHA256withECDSA", provider)
      signer.initSign(privateKey.privateKey)
      signer.update(msg)
      SecpECDSASignatureImpl(signer.sign())
    }

    def checkECDSASignature(msg: Array[Byte], sig: Array[Byte], pubKey: SecpPublicKey): Boolean = {
      val ecdsaVerify = Signature.getInstance("SHA256withECDSA", provider)
      ecdsaVerify.initVerify(pubKey.publicKey)
      ecdsaVerify.update(msg)
      ecdsaVerify.verify(sig)
    }
  }

  sealed trait Sha256Hash {
    def bytes: Vector[Byte]
    def hexEncoded: String = {
      bytes.map(byte => f"${byte & 0xff}%02x").mkString
    }

    override def equals(obj: Any): Boolean = obj match {
      case other: Sha256Hash => bytes == other.bytes
      case _ => false
    }

    override def hashCode(): Int = bytes.hashCode()
  }

  private[crypto] case class Sha256HashImpl(bytes: Vector[Byte]) extends Sha256Hash {
    require(bytes.size == 32)
  }

  object Sha256Hash {

    def fromBytes(arr: Array[Byte]): Sha256Hash = Sha256HashImpl(arr.toVector)

    def compute(bArray: Array[Byte]): Sha256Hash = {
      Sha256HashImpl(
        MessageDigest
          .getInstance("SHA-256")
          .digest(bArray)
          .toVector
      )
    }

    def fromHex(hexedBytes: String): Sha256Hash = {
      val HEX_STRING_RE = "^[0-9a-fA-F]{64}$".r
      if (HEX_STRING_RE.matches(hexedBytes)) Sha256HashImpl(hexToBytes(hexedBytes))
      else
        throw new IllegalArgumentException(
          "The given hex string doesn't correspond to a valid SHA-256 hash encoded as string"
        )
    }

    private def hexToBytes(hex: String): Vector[Byte] = {
      val HEX_ARRAY = "0123456789abcdef".toCharArray
      for {
        pair <- hex.grouped(2).toVector
        firstIndex = HEX_ARRAY.indexOf(pair(0))
        secondIndex = HEX_ARRAY.indexOf(pair(1))
        octet = firstIndex << 4 | secondIndex
      } yield octet.toByte
    }
  }
}
