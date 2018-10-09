package io.iohk.cef

import io.iohk.cef.crypto.encryption.EncryptionAlgorithmsCollection
import io.iohk.cef.crypto.hashing.HashingAlgorithmsCollection
import io.iohk.cef.crypto.signing.SigningAlgorithmsCollection

/**
  * Collection of all the high level cryptographic methods and data types
  *
  * Some examples on how the crypto package works
  * {{{
  *
  * >>> import io.iohk.cef.crypto._
  *
  * >>> case class User(name: String, age: Int)
  *
  * >>> val user = User("Foo Bar", 42)
  * >>> val user2 = User("Bar Foo", 24)
  *
  * # HASHING
  * >>> val userHash = hash(user)
  * >>> isValidHash(user, userHash)
  * true
  *
  * >>> isValidHash(user2, userHash)
  * false
  *
  * # ENCRYPTION
  * >>> val EncryptionKeyPair(pubEncryptionKey, privEncryptionKey) = generateEncryptionKeyPair
  * >>> val encrypted = encrypt(user, pubEncryptionKey)
  * >>> decrypt[User](encrypted, privEncryptionKey)
  * Right(User(Foo Bar,42))
  *
  * # SIGNING
  * >>> val SigningKeyPair(pubSigningKey, privSigningKey) = generateSigningKeyPair
  * >>> val signature = sign(user, privSigningKey)
  * >>> isValidSignature(user, signature, pubSigningKey)
  * true
  *
  * >>> isValidSignature(user2, signature, pubSigningKey)
  * false
  *
  * }}}
  */
package object crypto extends Crypto {

  // CONFIGURATION

  private val secureRandom = new java.security.SecureRandom

  protected override val hashingCollection: HashingAlgorithmsCollection =
    HashingAlgorithmsCollection()
  protected override val hashingType: hashingCollection.HashingAlgorithmType =
    hashingCollection.HashingAlgorithmType.SHA256

  protected override val encryptionAlgorithmsCollection: EncryptionAlgorithmsCollection =
    EncryptionAlgorithmsCollection(secureRandom)
  protected override val defaultEncryptionType: encryptionAlgorithmsCollection.EncryptionAlgorithmType =
    encryptionAlgorithmsCollection.EncryptionAlgorithmType.RSA

  protected override val signingAlgorithmsCollection: SigningAlgorithmsCollection =
    SigningAlgorithmsCollection(secureRandom)
  protected override val defaultSigningType: signingAlgorithmsCollection.SigningAlgorithmType =
    signingAlgorithmsCollection.SigningAlgorithmType.SHA256withRSA

}
