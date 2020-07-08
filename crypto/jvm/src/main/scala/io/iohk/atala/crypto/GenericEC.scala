package io.iohk.atala.crypto

import java.security._
import java.security.spec.{
  KeySpec,
  ECGenParameterSpec => JavaECGenParameterSpec,
  ECParameterSpec => JavaECParameterSpec,
  ECPoint => JavaECPoint,
  ECPrivateKeySpec => JavaECPrivateKeySpec,
  ECPublicKeySpec => JavaECPublicKeySpec
}

import io.iohk.atala.crypto.ECConfig.{CURVE_FIELD_BYTE_SIZE, CURVE_NAME, SIGNATURE_ALGORITHM}

/**
  * Generic implementation of {@link ECTrait}.
  *
  * This has bouncycastle-agnostic code, so that we can have a spongycastle implementation (just for Android),
  * and the default implementation (bouncycastle), the goal is to remove as much duplication as possible.
  */
abstract class GenericEC(proviver: java.security.Provider) extends ECTrait {
  Security.addProvider(proviver)

  require(CURVE_FIELD_BYTE_SIZE == (getCurveFieldSize + 7) / 8)

  private val keyFactory = KeyFactory.getInstance("EC", proviver)
  protected val ecNamedCurveSpec: JavaECParameterSpec
  protected def keySpec(d: BigInt): KeySpec
  protected def getCurveFieldSize: Int

  override def generateKeyPair(): ECKeyPair = {
    val keyGen = KeyPairGenerator.getInstance("ECDSA", proviver)
    val ecSpec = new JavaECGenParameterSpec(CURVE_NAME)
    keyGen.initialize(ecSpec, new SecureRandom())
    val keyPair = keyGen.generateKeyPair()
    ECKeyPair(new JvmECPrivateKey(keyPair.getPrivate), new JvmECPublicKey(keyPair.getPublic))
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
    val pubSpec = keySpec(d)
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
        val signer = Signature.getInstance(SIGNATURE_ALGORITHM, proviver)
        signer.initSign(key.key)
        signer.update(data)
        ECSignature(signer.sign())
    }
  }

  override def verify(data: Array[Byte], publicKey: ECPublicKey, signature: ECSignature): Boolean = {
    publicKey match {
      case key: JvmECPublicKey =>
        val verifier = Signature.getInstance(SIGNATURE_ALGORITHM, proviver)
        verifier.initVerify(key.key)
        verifier.update(data)
        verifier.verify(signature.data)
    }
  }
}
