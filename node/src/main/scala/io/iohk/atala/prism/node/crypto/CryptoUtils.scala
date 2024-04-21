package io.iohk.atala.prism.node.crypto

import io.iohk.atala.prism.node.models.ProtocolConstants
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util
import org.bouncycastle.jce.interfaces.{ECPrivateKey, ECPublicKey}

import java.security.{KeyFactory, MessageDigest, PrivateKey, PublicKey, Security, Signature}
import org.bouncycastle.jce.{ECNamedCurveTable, ECPointUtil}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveSpec

import java.math.BigInteger
import java.security.spec.{ECParameterSpec, ECPoint, ECPrivateKeySpec, ECPublicKeySpec}

object CryptoUtils {
  def isSecp256k1(key: SecpPublicKey): Boolean = {
    val x = key.asInstanceOf[ECPublicKey].getQ.getAffineXCoord.toBigInteger
    val y = key.asInstanceOf[ECPublicKey].getQ.getAffineYCoord.toBigInteger

    val p: BigInt = BigInt("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16)
    val b = BigInt("7").bigInteger

    y.pow(2) == x.pow(3).add(b).mod(p)
  }

  trait SecpPublicKey {
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

  trait SecpECDSASignature {
    def bytes: Array[Byte]
  }
  private[crypto] case class SecpECDSASignatureImpl(bytes: Array[Byte]) extends SecpECDSASignature

  object SecpECDSASignature {
    def fromBytes(bytes: Array[Byte]): SecpECDSASignature = SecpECDSASignatureImpl(bytes)
  }

  object SecpPrivateKey {
    def unsafefromBytesCompressed(bytes: Array[Byte]): SecpPrivateKey = SecpPrivateKeyImpl(bytes)
  }

  trait SecpPrivateKey {
    private[crypto] def bytes: Array[Byte]
    private[crypto] def privateLey: PrivateKey
    def getEncoded: Array[Byte]
  }
  private[crypto] case class SecpPrivateKeyImpl(bytes: Array[Byte]) extends SecpPrivateKey {
    override def getEncoded: Array[Byte] = {
      // val PRIVATE_KEY_BYTE_SIZE = 32
      // val byteList = privateLey.asInstanceOf[ECPrivateKey].getD.toByteArray
      // val padding = Array.fill[Byte](PRIVATE_KEY_BYTE_SIZE - byteList.size) { 0 }
      // padding ++ byteList
      // The SDK was adding a padding that seems to later not be able to parse according to tests

      privateLey
        .asInstanceOf[ECPrivateKey]
        .getD
        .toByteArray
        .dropWhile(_ == 0)
    }

    override def privateLey: PrivateKey = {
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

  object SecpECDSA {
    def signBytes(msg: Array[Byte], privateKey: SecpPrivateKey): SecpECDSASignature = {
      val signer = Signature.getInstance("ECDSA", provider)
      signer.initSign(privateKey.privateLey)
      signer.update(msg)
      SecpECDSASignatureImpl(signer.sign())
    }
  }

  // We define the constructor to SecpKeys private so that the only way to generate
  // these keys is by using the methods unsafeToPublicKeyFromByteCoordinates and
  // unsafeToPublicKeyFromCompressed.
  object SecpPublicKey {

    private[crypto] def fromPublicKey(key: PublicKey): SecpPublicKey = new SecpPublicKeyImpl(key)

    // move to SecpECDSA object
    def checkECDSASignature(msg: Array[Byte], sig: Array[Byte], pubKey: SecpPublicKey): Boolean = {
      val ecdsaVerify = Signature.getInstance("SHA256withECDSA", provider)
      ecdsaVerify.initVerify(pubKey.publicKey)
      ecdsaVerify.update(msg)
      ecdsaVerify.verify(sig)
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
      SecpPublicKey.unsafeToSecpPublicKeyFromBigIntegerCoordinates(xInteger, yInteger)
    }

    def unsafeToSecpPublicKeyFromBigIntegerCoordinates(x: BigInt, y: BigInt): SecpPublicKey = {
      val params = ECNamedCurveTable.getParameterSpec("secp256k1")
      val fact = KeyFactory.getInstance("ECDSA", provider)
      val curve = params.getCurve
      val ellipticCurve = EC5Util.convertCurve(curve, params.getSeed)
      val point = new ECPoint(x.bigInteger, y.bigInteger)
      val params2 = EC5Util.convertSpec(ellipticCurve, params)
      val keySpec = new ECPublicKeySpec(point, params2)
      SecpPublicKey.fromPublicKey(fact.generatePublic(keySpec))
    }

    def unsafetoPublicKeyFromUncompressed(bytes: Array[Byte]): SecpPublicKey = {
      val PRIVATE_KEY_BYTE_SIZE: Int = 32
      val pointSize = PRIVATE_KEY_BYTE_SIZE * 2  + 1
      require(bytes.length == pointSize, s"Invalid public key bytes length, ${bytes.length}")

      val uncompressedPrefix = bytes.head
      require(uncompressedPrefix == 0x04.toByte, "Invalid uncompressed public key prefix")

      val xBytes = bytes.slice(1, pointSize / 2 + 1)
      val yBytes = bytes.slice(pointSize / 2 + 1, pointSize + 1)

      val x = new BigInteger(1, xBytes)
      val y = new BigInteger(1, yBytes)
      unsafeToSecpPublicKeyFromBigIntegerCoordinates(x, y)
    }
  }

  private val provider = new BouncyCastleProvider()
  private val PUBLIC_KEY_COORDINATE_BYTE_SIZE: Int = 32

  Security.addProvider(provider)

  trait Sha256Hash {
    def bytes: Vector[Byte]
    def hexEncoded: String = bytesToHex(bytes)
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
  }

  def bytesToHex(bytes: Vector[Byte]): String = {
    bytes.map(byte => f"${byte & 0xff}%02x").mkString
  }

  def hexToBytes(hex: String): Vector[Byte] = {
    val HEX_ARRAY = "0123456789abcdef".toCharArray
    for {
      pair <- hex.grouped(2).toVector
      firstIndex = HEX_ARRAY.indexOf(pair(0))
      secondIndex = HEX_ARRAY.indexOf(pair(1))
      octet = firstIndex << 4 | secondIndex
    } yield octet.toByte
  }
}
