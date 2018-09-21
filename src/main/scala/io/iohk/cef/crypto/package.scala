package io.iohk.cef

import io.iohk.cef.network.encoding.{Encoder => LowLevelEncoder}
import io.iohk.cef.network.encoding.{Decoder => LowLevelDecoder}
import io.iohk.cef.network.encoding.nio.NioEncoder
import io.iohk.cef.network.encoding.nio.NioDecoder
import akka.util.ByteString
import io.iohk.cef.crypto.hashing.HashingAlgorithmsCollection
import io.iohk.cef.crypto.signing.SigningAlgorithmsCollection
import io.iohk.cef.crypto.encryption.EncryptionAlgorithmsCollection

/**
  * Collection of all the high level cryptographic methods and data types
  *
  * Some examples on how the crypto package works
  * {{{
  *
  * >>> import io.iohk.cef.crypto._
  * >>> import io.iohk.cef.network.encoding.nio._
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

package crypto {

  /**
    * Base type that allows the creation of custom versions of a `Crypto` package, changing
    * the choosen `secureRandom` algorithm and the default `hashing`, `encryption` and `signing`
    * algorithms
    */
  trait Crypto extends Hashing with Encryption with Signing with EncodingHelpers

  // FIXME: This extensions to the encoders library should go into the encoders package
  trait EncodingHelpers {

    type Encoder[T] = LowLevelEncoder[T, ByteString]
    type Decoder[T] = LowLevelDecoder[ByteString, T]

    implicit def EncoderFromNIOEncoder[T](implicit nioEncoder: NioEncoder[T]): Encoder[T] =
      (t: T) => ByteString(nioEncoder.encode(t))

    implicit def DecoderFromNIODecoder[T](implicit nioDecoder: NioDecoder[T]): Decoder[T] =
      (u: ByteString) => nioDecoder.decode(u.toByteBuffer)

    implicit val ByteStringIdentityEncoder: Encoder[ByteString] = (bs: ByteString) => bs

    implicit val ByteStringIdentityDecoder: Decoder[ByteString] = (bs: ByteString) => Some(bs)

  }

  // FIXME: At some point, it would be ideal to identify the different things that can go wrong
  // on an actual implementation of the decoding of a key, and represent them in here (and make
  // the actual algorithms respect it)
  /**
    * ADT representing the types of errors that can happen trying to decode a cryptographic key
    * on a concrete, low level, algorithm implementation.
    */
  sealed trait KeyDecodingError
  object KeyDecodingError {

    /** Catch all, that contains a description of the problem the low level algorithm has found */
    case class UnderlayingImplementationError(description: String) extends KeyDecodingError
  }
}
