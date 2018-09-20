package io.iohk.cef.crypto
package signing

import io.iohk.cef.crypto.KeyDecodingError
import akka.util.ByteString

/** Defines a contract on how a signing algorithm should be implemented */
private[crypto] trait SigningAlgorithm {

  /** The type used internally in the algorithm to represent a public key */
  type PublicKey

  /** The type used internally in the algorithm to represent a private key */
  type PrivateKey

  /** Returns a signature of the provided `source` bytes */
  def sign(source: ByteString, key: PrivateKey): SignatureBytes

  /** Checks wether the provided `signature` is, in fact, a signature of the provided `source` bytes */
  def isSignatureValid(signature: SignatureBytes, source: ByteString, key: PublicKey): Boolean

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

case class SignatureBytes(bytes: ByteString)
case class PublicKeyBytes(bytes: ByteString)
case class PrivateKeyBytes(bytes: ByteString)
