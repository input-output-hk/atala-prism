package io.iohk.cef.crypto
package signing
package algorithms

import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, KeyPairGenerator, SecureRandom, Signature => JSignature}

import akka.util.ByteString

class SHA256withRSA(secureRandom: SecureRandom) extends SigningAlgorithm {

  private val KeyAlgorithm = "RSA"
  private val Algorithm = "SHA256withRSA"

  type PublicKey = java.security.PublicKey
  type PrivateKey = java.security.PrivateKey

  override def sign(source: ByteString, key: PrivateKey): SignatureBytes = {
    val signer = JSignature.getInstance(Algorithm)
    signer.initSign(key)

    // TODO: Find a way to use buffers in order to not crash on huge inputs
    signer.update(source.toArray)

    val result = signer.sign()
    SignatureBytes(ByteString(result))
  }

  override def isSignatureValid(signature: SignatureBytes, source: ByteString, key: PublicKey): Boolean = {
    val signer = JSignature.getInstance(Algorithm)
    signer.initVerify(key)

    // TODO: Find a way to use buffers in order to not crash on huge inputs
    signer.update(source.toArray)

    try {
      signer.verify(signature.bytes.toArray)
    } catch {
      case _: Throwable => false
    }
  }

  override def generateKeyPair(): (PublicKey, PrivateKey) = {
    val generator = KeyPairGenerator.getInstance("RSA")
    generator.initialize(2048)
    val keyPair = generator.genKeyPair()

    (keyPair.getPublic, keyPair.getPrivate)
  }

  override def encodePublicKey(key: PublicKey): PublicKeyBytes =
    PublicKeyBytes(ByteString(key.getEncoded))

  override def encodePrivateKey(key: PrivateKey): PrivateKeyBytes =
    PrivateKeyBytes(ByteString(key.getEncoded))

  override def decodePublicKey(bytes: PublicKeyBytes): Either[KeyDecodingError, PublicKey] = {
    try {
      val publicKeySpec = new X509EncodedKeySpec(bytes.bytes.toArray)
      val keyFactory = KeyFactory.getInstance("RSA")
      Right(keyFactory.generatePublic(publicKeySpec))
    } catch {
      case t: Throwable =>
        Left(KeyDecodingError.UnderlayingImplementationError(t.getMessage))
    }
  }

  override def decodePrivateKey(bytes: PrivateKeyBytes): Either[KeyDecodingError, PrivateKey] = {
    try {
      val privateKeySpec = new PKCS8EncodedKeySpec(bytes.bytes.toArray)
      val keyFactory = KeyFactory.getInstance("RSA")
      Right(keyFactory.generatePrivate(privateKeySpec))
    } catch {
      case t: Throwable =>
        Left(KeyDecodingError.UnderlayingImplementationError(t.getMessage))
    }
  }

  override def toPublicKey(obj: AnyRef): Option[PublicKey] = obj match {
    case key: PublicKey if key.getAlgorithm == KeyAlgorithm => Some(key)
    case _ => None
  }
}
