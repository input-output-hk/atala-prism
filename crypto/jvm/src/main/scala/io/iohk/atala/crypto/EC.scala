package io.iohk.atala.crypto

import java.security.spec.{
  ECGenParameterSpec => JavaECGenParameterSpec,
  ECPoint => JavaECPoint,
  ECPrivateKeySpec => JavaECPrivateKeySpec,
  ECPublicKeySpec => JavaECPublicKeySpec
}
import java.security.{KeyFactory, KeyPairGenerator, SecureRandom, Security, Signature}

import io.iohk.atala.crypto.ECConfig.{CURVE_FIELD_BYTE_SIZE, CURVE_NAME, SIGNATURE_ALGORITHM}
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.{ECNamedCurveSpec, ECPublicKeySpec => BCECPublicKeySpec}

/**
  * JVM implementation of {@link ECTrait}.
  */
object EC extends ECTrait {
  Security.addProvider(new BouncyCastleProvider)

  private val bouncyCastleProvider = "BC"
  private val keyFactory = KeyFactory.getInstance("EC", bouncyCastleProvider)
  private val ecParameterSpec = ECNamedCurveTable.getParameterSpec(CURVE_NAME)

  require(CURVE_FIELD_BYTE_SIZE == (ecParameterSpec.getCurve.getFieldSize + 7) / 8)

  private val ecNamedCurveSpec = new ECNamedCurveSpec(
    ecParameterSpec.getName,
    ecParameterSpec.getCurve,
    ecParameterSpec.getG,
    ecParameterSpec.getN
  )

  override def generateKeyPair(): ECKeyPair = {
    val keyGen = KeyPairGenerator.getInstance("ECDSA", bouncyCastleProvider)
    val ecSpec = new JavaECGenParameterSpec(CURVE_NAME)
    keyGen.initialize(ecSpec, new SecureRandom())
    JvmECKeyPair(keyGen.generateKeyPair())
  }

  override def toPrivateKey(d: BigInt): ECPrivateKey = {
    val spec = toPrivateKeySpec(d)
    new JvmECPrivateKey(keyFactory.generatePrivate(spec))
  }

  override def toPublicKey(x: BigInt, y: BigInt): ECPublicKey = {
    val spec = toPublicKeySpec(x, y)
    new JvmECPublicKey(keyFactory.generatePublic(spec))
  }

  override def toPublicKeyFromPrivateKey(d: BigInt): ECPublicKey = {
    val Q = ecParameterSpec.getG.multiply(d.bigInteger)
    val pubSpec = new BCECPublicKeySpec(Q, ecParameterSpec)
    new JvmECPublicKey(keyFactory.generatePublic(pubSpec))
  }

  private def toPrivateKeySpec(d: BigInt): JavaECPrivateKeySpec = {
    new JavaECPrivateKeySpec(d.bigInteger, ecNamedCurveSpec)
  }

  private def toPublicKeySpec(x: BigInt, y: BigInt): JavaECPublicKeySpec = {
    val ecPoint = new JavaECPoint(x.bigInteger, y.bigInteger)
    new JavaECPublicKeySpec(ecPoint, ecNamedCurveSpec)
  }

  override def sign(data: Array[Byte], privateKey: ECPrivateKey): ECSignature = {
    privateKey match {
      case key: JvmECPrivateKey =>
        val signer = Signature.getInstance(SIGNATURE_ALGORITHM, bouncyCastleProvider)
        signer.initSign(key.key)
        signer.update(data)
        ECSignature(signer.sign())
    }
  }

  override def verify(data: Array[Byte], publicKey: ECPublicKey, signature: ECSignature): Boolean = {
    publicKey match {
      case key: JvmECPublicKey =>
        val verifier = Signature.getInstance(SIGNATURE_ALGORITHM, bouncyCastleProvider)
        verifier.initVerify(key.key)
        verifier.update(data)
        verifier.verify(signature.data)
    }
  }
}
