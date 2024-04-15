package identus.apollo

// import io.iohk.atala.shared.models.HexString
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveSpec
// import zio._

import java.security.KeyFactory
import java.security.spec.{ECPrivateKeySpec, ECPublicKeySpec}
import scala.util.Try
import com.google.protobuf.ByteString

trait Apollo {
  def secp256k1: Secp256k1KeyOps
  def ed25519: Ed25519KeyOps
  def x25519: X25519KeyOps
}

object Apollo {
  // def layer: ULayer[Apollo] = ZLayer.succeed(default)

  // TODO: migrate to KMP Apollo and support other key types
  def default: Apollo = new Apollo {
    override def secp256k1: Secp256k1KeyOps = Prism14Secp256k1Ops
    override def ed25519: Ed25519KeyOps = ???
    override def x25519: X25519KeyOps = ???
  }
}

trait Encodable {
  def getEncoded: Array[Byte]
}

trait Signable {
  def sign(data: Array[Byte]): Array[Byte]
}

trait Verifiable {
  def verify(data: Array[Byte], signature: Array[Byte]): Try[Unit]
}

trait PublicKey extends Encodable
trait PrivateKey extends Encodable {
  type Pub <: PublicKey
  def toPublicKey: Pub
  override final def toString(): String = "<REDACTED>"

  def curveName: String
  def toCurvePoint: CurvePoint // FIXME
  def getX: Array[Byte] = toCurvePoint.x
  def getY: Array[Byte] = toCurvePoint.y

  def getXAsByteString = ByteString.copyFrom(getX)
  def getYAsByteString = ByteString.copyFrom(getY)

  // TODO rename this
  def getEncodedCompressed: Array[Byte] = ???
  def getEncodedCompressedAsByteString = ByteString.copyFrom(getEncodedCompressed)
}
trait MyKeyPair {
  def publicKey: PublicKey
  def privateKey: PrivateKey
}

case class CurvePoint(x: Array[Byte], y: Array[Byte])

//FIXME
// enum DerivationPath {
//   case Normal(i: Int) extends DerivationPath
//   case Hardened(i: Int) extends DerivationPath
// }

final case class ECPoint(x: BigInt, y: BigInt)

// secp256k1
final case class Secp256k1KeyPair(publicKey: Secp256k1PublicKey, privateKey: Secp256k1PrivateKey) extends MyKeyPair
trait Secp256k1PublicKey extends PublicKey with Verifiable {
  def getEncodedCompressed: Array[Byte]

  def getEncodedUncompressed: Array[Byte]

  def getECPoint: ECPoint

  override final def hashCode(): Int = HexString.fromByteArray(getEncoded).hashCode()

  override final def equals(x: Any): Boolean = x match {
    case otherPK: Secp256k1PublicKey =>
      HexString.fromByteArray(this.getEncoded) == HexString.fromByteArray(otherPK.getEncoded)
    case _ => false
  }

  def toJavaPublicKey: java.security.interfaces.ECPublicKey = {
    val point = getECPoint
    val x = point.x
    val y = point.y
    val keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider())
    val ecParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
    val ecNamedCurveSpec = new ECNamedCurveSpec(
      ecParameterSpec.getName(),
      ecParameterSpec.getCurve(),
      ecParameterSpec.getG(),
      ecParameterSpec.getN()
    )
    val ecPublicKeySpec =
      new ECPublicKeySpec(new java.security.spec.ECPoint(x.bigInteger, y.bigInteger), ecNamedCurveSpec)
    keyFactory.generatePublic(ecPublicKeySpec).asInstanceOf[java.security.interfaces.ECPublicKey]
  }
}
trait Secp256k1PrivateKey extends PrivateKey with Signable {
  type Pub = Secp256k1PublicKey

  override final def hashCode(): Int = HexString.fromByteArray(getEncoded).hashCode()

  override final def equals(x: Any): Boolean = x match {
    case otherPK: Secp256k1PrivateKey =>
      HexString.fromByteArray(this.getEncoded) == HexString.fromByteArray(otherPK.getEncoded)
    case _ => false
  }

  def toJavaPrivateKey: java.security.interfaces.ECPrivateKey = {
    val bytes = getEncoded
    val keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider())
    val ecParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
    val ecNamedCurveSpec = new ECNamedCurveSpec(
      ecParameterSpec.getName(),
      ecParameterSpec.getCurve(),
      ecParameterSpec.getG(),
      ecParameterSpec.getN()
    )
    val ecPrivateKeySpec = new ECPrivateKeySpec(new java.math.BigInteger(1, bytes), ecNamedCurveSpec)
    keyFactory.generatePrivate(ecPrivateKeySpec).asInstanceOf[java.security.interfaces.ECPrivateKey]
  }
}
trait Secp256k1KeyOps {
  def publicKeyFromEncoded(bytes: Array[Byte]): Try[Secp256k1PublicKey]
  def privateKeyFromEncoded(bytes: Array[Byte]): Try[Secp256k1PrivateKey]
  // def generateKeyPair: UIO[Secp256k1KeyPair]
  // def randomBip32Seed: UIO[(Array[Byte], Seq[String])]
  // def deriveKeyPair(seed: Array[Byte])(path: DerivationPath*): UIO[Secp256k1KeyPair]
}

// ed25519
final case class Ed25519KeyPair(publicKey: Ed25519PublicKey, privateKey: Ed25519PrivateKey)
trait Ed25519PublicKey extends PublicKey
trait Ed25519PrivateKey extends PrivateKey with Signable {
  type Pub = Ed25519PublicKey
}
trait Ed25519KeyOps {
  def publicKeyFromEncoded(bytes: Array[Byte]): Try[Ed25519PublicKey]
  def privateKeyFromEncoded(bytes: Array[Byte]): Try[Ed25519PrivateKey]
}

// x25519
final case class X25519KeyPair(publicKey: X25519PublicKey, privateKey: X25519PrivateKey)
trait X25519PublicKey extends PublicKey
trait X25519PrivateKey extends PrivateKey {
  type Pub = X25519PublicKey
}
trait X25519KeyOps {
  def publicKeyFromEncoded(bytes: Array[Byte]): Try[X25519PublicKey]
  def privateKeyFromEncoded(bytes: Array[Byte]): Try[X25519PrivateKey]
}
