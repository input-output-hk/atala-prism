package io.iohk.cef.crypto
package encryption

import io.iohk.cef.crypto.KeyDecodingError
import akka.util.ByteString

private[crypto] trait EncryptionAlgorithm {

  type PublicKey
  type PrivateKey

  def encrypt(source: ByteString, key: PublicKey): EncryptedBytes

  def decrypt(source: EncryptedBytes, key: PrivateKey): Either[DecryptError, ByteString]

  def generateKeyPair(): (PublicKey, PrivateKey)

  def encodePublicKey(key: PublicKey): PublicKeyBytes

  def encodePrivateKey(key: PrivateKey): PrivateKeyBytes

  def decodePublicKey(bytes: PublicKeyBytes): Either[KeyDecodingError, PublicKey]

  def decodePrivateKey(bytes: PrivateKeyBytes): Either[KeyDecodingError, PrivateKey]

}

case class PublicKeyBytes(bytes: ByteString)
case class PrivateKeyBytes(bytes: ByteString)
case class EncryptedBytes(bytes: ByteString)

trait DecryptError
object DecryptError {
  case class UnderlayingImplementationError(description: String) extends DecryptError
}
