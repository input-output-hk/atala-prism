package io.iohk.cef.crypto
package signing

import io.iohk.cef.crypto.KeyDecodingError
import akka.util.ByteString

private[crypto] trait SigningAlgorithm {

  type PublicKey
  type PrivateKey

  def sign(source: ByteString, key: PrivateKey): SignatureBytes

  def isSignatureValid(signature: SignatureBytes, source: ByteString, key: PublicKey): Boolean

  def generateKeyPair(): (PublicKey, PrivateKey)

  def encodePublicKey(key: PublicKey): PublicKeyBytes

  def encodePrivateKey(key: PrivateKey): PrivateKeyBytes

  def decodePublicKey(bytes: PublicKeyBytes): Either[KeyDecodingError, PublicKey]

  def decodePrivateKey(bytes: PrivateKeyBytes): Either[KeyDecodingError, PrivateKey]

}

case class SignatureBytes(bytes: ByteString)
case class PublicKeyBytes(bytes: ByteString)
case class PrivateKeyBytes(bytes: ByteString)
