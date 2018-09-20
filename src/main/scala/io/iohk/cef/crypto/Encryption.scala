package io.iohk.cef.crypto

import akka.util.ByteString

import io.iohk.cef.crypto.encryption._
import io.iohk.cef.crypto.encoding.TypedByteString
import io.iohk.cef.crypto.encoding.TypedByteStringDecodingError

trait Encryption {

  // PARAMETERS

  protected val encryptionAlgorithmsCollection: EncryptionAlgorithmsCollection
  protected val defaultEncryptionType: encryptionAlgorithmsCollection.EncryptionAlgorithmType

  def generateEncryptionKeyPair(): EncryptionKeyPair = {
    val (llPub, llPriv) = defaultEncryptionType.algorithm.generateKeyPair()
    EncryptionKeyPair(
      EncryptionPublicKey(defaultEncryptionType)(llPub),
      EncryptionPrivateKey(defaultEncryptionType)(llPriv))
  }

  def encrypt[T](entity: T, key: EncryptionPublicKey)(implicit encoder: Encoder[T]): EncryptedData = {
    val encryptedBytes =
      key.`type`.algorithm.encrypt(encoder.encode(entity), key.lowlevelKey)

    EncryptedData(key.`type`, encryptedBytes)
  }

  private[crypto] def decryptBytes(
      encryptedData: EncryptedData,
      key: EncryptionPrivateKey): Either[DecryptError, ByteString] = {
    if (encryptedData.`type` != key.`type`)
      Left(
        DecryptError
          .IncompatibleEncryptionAlgorithm(key.`type`.algorithmIdentifier, encryptedData.`type`.algorithmIdentifier))
    else
      key.`type`.algorithm.decrypt(encryptedData.bytes, key.lowlevelKey) match {
        case Left(e) => Left(DecryptError.UnderlayingDecryptionError(e))
        case Right(bytes) => Right(bytes)
      }
  }

  def decrypt[T](encryptedData: EncryptedData, key: EncryptionPrivateKey)(
      implicit decoder: Decoder[T]): Either[DecryptError, T] = {
    decryptBytes(encryptedData, key)
      .flatMap { bytes =>
        decoder.decode(bytes) match {
          case Some(e) => Right(e)
          case None => Left(DecryptError.EntityCouldNotBeDecoded)
        }
      }
  }

  trait EncryptionPublicKey {
    private[Encryption] val `type`: encryptionAlgorithmsCollection.EncryptionAlgorithmType
    private[Encryption] val lowlevelKey: `type`.algorithm.PublicKey
    def toByteString: ByteString =
      EncryptionPublicKey.encodeInto(this).toByteString
  }

  object EncryptionPublicKey {

    private[Encryption] def apply(tpe: encryptionAlgorithmsCollection.EncryptionAlgorithmType)(
        llk: tpe.algorithm.PublicKey) =
      new EncryptionPublicKey {
        override private[Encryption] val `type`: encryptionAlgorithmsCollection.EncryptionAlgorithmType =
          tpe
        override private[Encryption] val lowlevelKey: `type`.algorithm.PublicKey =
          // This is the only way to explain the compiler that `type` and `tpe` are the same thing
          llk.asInstanceOf[`type`.algorithm.PublicKey]
      }

    private[Encryption] def encodeInto(key: EncryptionPublicKey): TypedByteString =
      TypedByteString(key.`type`.algorithmIdentifier, key.`type`.algorithm.encodePublicKey(key.lowlevelKey).bytes)

    private[Encryption] def decodeFrom(
        tbs: TypedByteString): Either[EncryptionPublicKeyDecodeError, EncryptionPublicKey] = {
      encryptionAlgorithmsCollection(tbs.`type`) match {
        case Some(encryptionType) =>
          encryptionType.algorithm.decodePublicKey(PublicKeyBytes(tbs.bytes)) match {
            case Right(lowlevelKey) =>
              Right(EncryptionPublicKey(encryptionType)(lowlevelKey))
            case Left(decodingError) =>
              Left(EncryptionPublicKeyDecodeError.KeyDecodingError(decodingError))
          }
        case None =>
          Left(EncryptionPublicKeyDecodeError.UnsupportedAlgorithm(tbs.`type`))
      }
    }

    def decodeFrom(bytes: ByteString): Either[EncryptionPublicKeyDecodeError, EncryptionPublicKey] = {
      TypedByteString
        .decodeFrom(bytes)
        .left
        .map(e => EncryptionPublicKeyDecodeError.DataExtractionError(e))
        .flatMap(decodeFrom)
    }
  }

  trait EncryptionPrivateKey {
    private[Encryption] val `type`: encryptionAlgorithmsCollection.EncryptionAlgorithmType
    private[Encryption] val lowlevelKey: `type`.algorithm.PrivateKey
    def toByteString: ByteString =
      EncryptionPrivateKey.encodeInto(this).toByteString
  }

  object EncryptionPrivateKey {

    private[Encryption] def apply(tpe: encryptionAlgorithmsCollection.EncryptionAlgorithmType)(
        llk: tpe.algorithm.PrivateKey) =
      new EncryptionPrivateKey {
        override private[Encryption] val `type`: encryptionAlgorithmsCollection.EncryptionAlgorithmType =
          tpe
        override private[Encryption] val lowlevelKey: `type`.algorithm.PrivateKey =
          // This is the only way to explain the compiler that `type` and `tpe` are the same thing
          llk.asInstanceOf[`type`.algorithm.PrivateKey]
      }

    private[Encryption] def encodeInto(key: EncryptionPrivateKey): TypedByteString =
      TypedByteString(key.`type`.algorithmIdentifier, key.`type`.algorithm.encodePrivateKey(key.lowlevelKey).bytes)

    private[Encryption] def decodeFrom(
        tbs: TypedByteString): Either[EncryptionPrivateKeyDecodeError, EncryptionPrivateKey] = {
      encryptionAlgorithmsCollection(tbs.`type`) match {
        case Some(encryptionType) =>
          encryptionType.algorithm.decodePrivateKey(PrivateKeyBytes(tbs.bytes)) match {
            case Right(lowlevelKey) =>
              Right(EncryptionPrivateKey(encryptionType)(lowlevelKey))
            case Left(decodingError) =>
              Left(EncryptionPrivateKeyDecodeError.KeyDecodingError(decodingError))
          }
        case None =>
          Left(EncryptionPrivateKeyDecodeError.UnsupportedAlgorithm(tbs.`type`))
      }
    }

    def decodeFrom(bytes: ByteString): Either[EncryptionPrivateKeyDecodeError, EncryptionPrivateKey] = {
      TypedByteString
        .decodeFrom(bytes)
        .left
        .map(e => EncryptionPrivateKeyDecodeError.DataExtractionError(e))
        .flatMap(decodeFrom)
    }
  }

  case class EncryptionKeyPair(public: EncryptionPublicKey, `private`: EncryptionPrivateKey)

  class EncryptedData(
      private[Encryption] val `type`: encryptionAlgorithmsCollection.EncryptionAlgorithmType,
      private[Encryption] val bytes: EncryptedBytes) {
    def toByteString: ByteString =
      EncryptedData.encodeInto(this).toByteString
  }

  object EncryptedData {

    def apply(tpe: encryptionAlgorithmsCollection.EncryptionAlgorithmType, bytes: EncryptedBytes): EncryptedData =
      new EncryptedData(tpe, bytes)

    private[Encryption] def encodeInto(signature: EncryptedData): TypedByteString =
      TypedByteString(signature.`type`.algorithmIdentifier, signature.bytes.bytes)

    private[Encryption] def decodeFrom(tbs: TypedByteString): Either[EncryptedDataDecodeError, EncryptedData] = {
      encryptionAlgorithmsCollection(tbs.`type`) match {
        case Some(encryptionType) =>
          Right(new EncryptedData(encryptionType, EncryptedBytes(tbs.bytes)))
        case None =>
          Left(EncryptedDataDecodeError.UnsupportedAlgorithm(tbs.`type`))
      }
    }

    def decodeFrom(bytes: ByteString): Either[EncryptedDataDecodeError, EncryptedData] = {
      TypedByteString
        .decodeFrom(bytes)
        .left
        .map(e => EncryptedDataDecodeError.DataExtractionError(e))
        .flatMap(decodeFrom)
    }
  }

  sealed trait DecryptError
  object DecryptError {
    case class UnderlayingDecryptionError(underlaying: encryption.DecryptError) extends DecryptError
    case class IncompatibleEncryptionAlgorithm(keyAlgorithm: String, dataAlgorithm: String) extends DecryptError
    case object EntityCouldNotBeDecoded extends DecryptError
  }

  sealed trait EncryptionPublicKeyDecodeError
  object EncryptionPublicKeyDecodeError {
    case class DataExtractionError(cause: TypedByteStringDecodingError) extends EncryptionPublicKeyDecodeError
    case class UnsupportedAlgorithm(algorithmIdentifier: String) extends EncryptionPublicKeyDecodeError
    case class KeyDecodingError(cause: io.iohk.cef.crypto.KeyDecodingError) extends EncryptionPublicKeyDecodeError
  }

  sealed trait EncryptionPrivateKeyDecodeError
  object EncryptionPrivateKeyDecodeError {
    case class DataExtractionError(cause: TypedByteStringDecodingError) extends EncryptionPrivateKeyDecodeError
    case class UnsupportedAlgorithm(algorithmIdentifier: String) extends EncryptionPrivateKeyDecodeError
    case class KeyDecodingError(cause: io.iohk.cef.crypto.KeyDecodingError) extends EncryptionPrivateKeyDecodeError
  }

  sealed trait EncryptedDataDecodeError
  object EncryptedDataDecodeError {
    case class DataExtractionError(cause: TypedByteStringDecodingError) extends EncryptedDataDecodeError
    case class UnsupportedAlgorithm(algorithmIdentifier: String) extends EncryptedDataDecodeError
  }

}
