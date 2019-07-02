package io.iohk.crypto

import java.security._
import java.security.spec.{
  ECGenParameterSpec => JavaECGenParameterSpec,
  ECPoint => JavaECPoint,
  ECPrivateKeySpec => JavaECPrivateKeySpec,
  ECPublicKeySpec => JavaECPublicKeySpec
}

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.{ECNamedCurveSpec, ECPublicKeySpec => BCECPublicKeySpec}

object ECKeys {

  java.security.Security.addProvider(new BouncyCastleProvider)

  private val provider = "BC"
  private val curveName = "secp256k1"
  private val keyFactory = KeyFactory.getInstance("EC", provider)
  private val ecParameterSpec = ECNamedCurveTable.getParameterSpec(curveName)

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
    val ecSpec = new JavaECGenParameterSpec(curveName)
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
}
