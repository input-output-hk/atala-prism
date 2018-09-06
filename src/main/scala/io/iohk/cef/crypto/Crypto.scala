package io.iohk.cef.crypto

import akka.util.ByteString

abstract class Crypto {

//  // PARAMETERS
//  val hashCollection: HashAlgorithmsCollection
//  val hashAlgorithm: hashCollection.HashAlgorithmEntry
//
//  val cryptoCollection: CryptoAlgorithmsCollection
//  val cryptoAlgorithm: cryptoCollection.CryptoAlgorithmEntry
//
//  val signCollection: SignAlgorithmsCollection
//  val signAlgorithm: signCollection.SignAlgorithmEntry

  // HASHING
  def hashBytes(bytes: ByteString): Hash = ???
  def hashEntity[T](entity: T)(implicit encoder: Encoder[T]): Hash = ???
  def isValidHash[T](entity: T, hash: Hash)(implicit encoder: Encoder[T]): Boolean = ???

  sealed trait Hash

  object Hash {
    def decodeFrom(bytes: ByteString): Either[HashDecodeError, Hash] = ???
  }

  sealed trait HashDecodeError

  // ENCRYPTION
  def generateEncryptionKeyPair(): EncryptionKeyPair = ???
  def encryptBytes(bytes: ByteString, key: EncryptionPublicKey): EncryptedData = ???
  def encryptEntity[T](entity: T, key: EncryptionPublicKey)(implicit encoder: Encoder[T]): EncryptedData = ???

  def decryptEntity[T](encryptedData: EncryptedData, key: EncryptionPrivateKey)(
      implicit decoder: Decoder[T]): Either[DecryptError, T] = ???

  sealed trait EncryptionPublicKey
  object EncryptionPublicKey {
    def decodeFrom(bytes: ByteString): Either[EncryptionPublicKeyDecodeError, EncryptionPublicKey] = ???
  }
  sealed trait EncryptionPrivateKey
  object EncryptionPrivateKey {
    def decodeFrom(bytes: ByteString): Either[EncryptionPrivateKeyDecodeError, EncryptionPrivateKey] = ???
  }
  case class EncryptionKeyPair(public: EncryptionPublicKey, `private`: EncryptionPrivateKey)

  sealed trait EncryptedData

  sealed trait DecryptError
  sealed trait EncryptionPublicKeyDecodeError
  sealed trait EncryptionPrivateKeyDecodeError

  // SIGNING
  def generateSigningKeyPair(): SigningKeyPair = ???
  def signBytes(bytes: ByteString, key: SigningPrivateKey): Signature = ???
  def signEntity[T](t: T, key: SigningPrivateKey)(implicit encoder: Encoder[T]): Signature = ???
  def isValidSignatureOfBytes(bytes: ByteString, signature: Signature, key: SigningPublicKey): Boolean = ???
  def isValidSignature[T](t: T, signature: Signature, key: SigningPublicKey)(implicit encoder: Encoder[T]): Boolean =
    ???

  sealed trait SigningPublicKey
  object SigningPublicKey {
    def decodeFrom(bytes: ByteString): Either[DecodeSigningPublicKeyError, SigningPublicKey] = ???
  }
  sealed trait SigningPrivateKey
  object SigningPrivateKey {
    def decodeFrom(bytes: ByteString): Either[DecodeSigningPrivateKeyError, SigningPrivateKey] = ???
  }
  case class SigningKeyPair(public: SigningPublicKey, `private`: SigningPrivateKey)

  sealed trait Signature
  object Signature {
    def decodeFrom(bytes: ByteString): Either[DecodeSignatureError, Signature] = ???
  }

  sealed trait DecodeSignatureError
  sealed trait DecodeSigningPublicKeyError
  sealed trait DecodeSigningPrivateKeyError

}
