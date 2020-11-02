package io.iohk.atala.prism.crypto.japi

import java.math.BigInteger

import io.iohk.atala.prism.crypto.{ECTrait, GenericEC}

class ECFacade(val ec: GenericEC) extends EC {

  /**
    * Generates a P-256k/secp256k1/prime256v1 key-pair.
    */
  override def generateKeyPair(): ECKeyPair = new ECKeyPairFacade(ec.generateKeyPair())

  /**
    * Returns the key-pair represented by the given private key's `D` as byte array.
    */
  override def toKeyPairFromPrivateKey(d: Array[Byte]): ECKeyPair = {
    new ECKeyPairFacade(ec.toKeyPairFromPrivateKey(d))
  }

  /**
    * Returns the key-pair represented by the given private key's `D` as number.
    */
  override def toKeyPairFromPrivateKey(d: BigInteger): ECKeyPair = {
    new ECKeyPairFacade(ec.toKeyPairFromPrivateKey(BigInt(d)))
  }

  /**
    * Returns the private key represented by the given byte array.
    */
  override def toPrivateKey(d: Array[Byte]): ECPrivateKey = new ECPrivateKeyFacade(ec.toPrivateKey(d))

  /**
    * Returns the private key represented by the given number.
    */
  override def toPrivateKey(d: BigInteger): ECPrivateKey = new ECPrivateKeyFacade(ec.toPrivateKey(d))

  /**
    * Returns the public key represented by the given encoded byte array.
    */
  override def toPublicKey(encoded: Array[Byte]): ECPublicKey = new ECPublicKeyFacade(ec.toPublicKey(encoded))

  /**
    * Returns the public key represented by the given coordinates as byte arrays.
    */
  override def toPublicKey(x: Array[Byte], y: Array[Byte]): ECPublicKey = new ECPublicKeyFacade(ec.toPublicKey(x, y))

  /**
    * Returns the public key represented by the given coordinates.
    */
  override def toPublicKey(x: BigInteger, y: BigInteger): ECPublicKey = new ECPublicKeyFacade(ec.toPublicKey(x, y))

  /**
    * Returns the public key represented by the given private key's `D` as byte array.
    */
  override def toPublicKeyFromPrivateKey(d: Array[Byte]): ECPublicKey = {
    new ECPublicKeyFacade(ec.toPublicKeyFromPrivateKey(d))
  }

  /**
    * Returns the public key represented by the given private key's `D` as number.
    */
  override def toPublicKeyFromPrivateKey(d: BigInteger): ECPublicKey = {
    new ECPublicKeyFacade(ec.toPublicKeyFromPrivateKey(BigInt(d)))
  }

  /**
    * Signs the given text with the given private key.
    */
  override def sign(text: String, privateKey: ECPrivateKey): ECSignature = {
    new ECSignatureFacade(ec.sign(text, privateKey.asInstanceOf[ECPrivateKeyFacade].privateKey))
  }

  /**
    * Signs the given data with the given private key.
    */
  override def sign(data: Array[Byte], privateKey: ECPrivateKey): ECSignature = {
    new ECSignatureFacade(ec.sign(data, privateKey.asInstanceOf[ECPrivateKeyFacade].privateKey))
  }

  /**
    * Verifies whether the given text matches the given signature with the given public key.
    */
  override def verify(text: String, publicKey: ECPublicKey, signature: ECSignature): Boolean = {
    ec.verify(
      text,
      publicKey.asInstanceOf[ECPublicKeyFacade].publicKey,
      signature.asInstanceOf[ECSignatureFacade].signature
    )
  }

  /**
    * Verifies whether the given data matches the given signature with the given public key.
    */
  override def verify(data: Array[Byte], publicKey: ECPublicKey, signature: ECSignature): Boolean = {
    ec.verify(
      data,
      publicKey.asInstanceOf[ECPublicKeyFacade].publicKey,
      signature.asInstanceOf[ECSignatureFacade].signature
    )
  }
}

object ECFacade {
  def unwrap(ec: EC): ECTrait = ec.asInstanceOf[ECFacade].ec
}
