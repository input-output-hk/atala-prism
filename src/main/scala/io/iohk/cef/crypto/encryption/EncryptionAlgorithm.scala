package io.iohk.cef.crypto
package encryption

import io.iohk.cef.crypto.KeyDecodingError
import akka.util.ByteString

/** Defines a contract on how an encryption algorithm (encrypt/decrypt) should be implemented */
private[crypto] trait EncryptionAlgorithm {

  /** The type used internally in the algorithm to represent a public key */
  type PublicKey

  /** The type used internally in the algorithm to represent a private key */
  type PrivateKey

  /** Returns an encrypted version of `source`, wrapped in an [[EncryptedBytes]] entity*/
  def encrypt(source: ByteString, key: PublicKey): EncryptedBytes

  /** Returns a decrypted version of `source`, or a [[DecryptError]] if something has gone wrong */
  def decrypt(source: EncryptedBytes, key: PrivateKey): Either[DecryptError, ByteString]

  /** Returns a pair containing a public key and it's private counterpart */
  def generateKeyPair(): (PublicKey, PrivateKey)

  /** converts the public `key` into a [[akka.util.ByteString]], wrapped into a [[PublicKeyBytes]] entity */
  def encodePublicKey(key: PublicKey): PublicKeyBytes

  /** converts the private `key` into a [[akka.util.ByteString]], wrapped into a [[PrivateKeyBytes]] entity */
  def encodePrivateKey(key: PrivateKey): PrivateKeyBytes

  /** returns a decoded version of a [[PublicKey]], or an error if the bytes don't hold the proper information */
  def decodePublicKey(bytes: PublicKeyBytes): Either[KeyDecodingError, PublicKey]

  /** returns a decoded version of a [[PrivateKey]], or an error if the bytes don't hold the proper information */
  def decodePrivateKey(bytes: PrivateKeyBytes): Either[KeyDecodingError, PrivateKey]

}

case class PublicKeyBytes(bytes: ByteString)
case class PrivateKeyBytes(bytes: ByteString)
case class EncryptedBytes(bytes: ByteString)

trait DecryptError
object DecryptError {
  case class UnderlayingImplementationError(description: String) extends DecryptError
}
