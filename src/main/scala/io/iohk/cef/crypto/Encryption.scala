package io.iohk.cef.crypto

import akka.util.ByteString

import io.iohk.cef.crypto.encryption._
import io.iohk.cef.crypto.encoding.TypedByteString
import io.iohk.cef.crypto.encoding.TypedByteStringDecodingError

trait Encryption {

  // PARAMETERS

  protected val encryptionAlgorithmsCollection: EncryptionAlgorithmsCollection
  protected val defaultEncryptionType: encryptionAlgorithmsCollection.EncryptionAlgorithmType

  /**
    * Generates a pair of encrypting keys, using the default encryption algorithm. These
    * keys will identify, too, the encryption algorithm they can work with.
    *
    * @return a pair of encryption keys
    */
  def generateEncryptionKeyPair(): EncryptionKeyPair = {
    val (llPub, llPriv) = defaultEncryptionType.algorithm.generateKeyPair()
    EncryptionKeyPair(
      EncryptionPublicKey(defaultEncryptionType)(llPub),
      EncryptionPrivateKey(defaultEncryptionType)(llPriv))
  }

  /**
    * Generates an encrypted version of `entity`, using the `key` public key and the algorithm
    * supported by the `key`
    *
    * @tparam T        the type of `entity`
    *
    * @param  entity   the entity that needs to be encrypted
    * @param  key      the key to be used. It also identifies the encryption algorithm
    *                  to use
    * @param  encoder  how to convert `entity` into a stream of bytes
    *
    * @return          an encrypted version of `entity`
    */
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

  /**
    * Tries to restore an entity of type `T`, from `encryptedData`  using the `key`
    * private key and the algorithm supported by the `key`
    *
    * @tparam T              the type we want to restore the `encryptedData` into
    *
    * @param  encryptedData  the data we want to decrypt
    * @param  key            the key to be used. It also identifies the encryption algorithm
    *                        to use
    * @param  decoder        how to try and restore a (decrypted) ByteString into an entity
    *                        of type `T`
    *
    * @return                some sort of error or the restored entity of type `T`
    */
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

  /** Data entity containing a encryption algorithm identifier and a public key for that algorithm */
  trait EncryptionPublicKey {
    private[Encryption] val `type`: encryptionAlgorithmsCollection.EncryptionAlgorithmType
    private[Encryption] val lowlevelKey: `type`.algorithm.PublicKey

    /** Encodes this key, including the algorithm identifier, into a ByteString */
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

    /** Tries to restore a `EncryptionPublicKey` from the content of the `bytes` ByteString */
    def decodeFrom(bytes: ByteString): Either[EncryptionPublicKeyDecodeError, EncryptionPublicKey] = {
      TypedByteString
        .decodeFrom(bytes)
        .left
        .map(e => EncryptionPublicKeyDecodeError.DataExtractionError(e))
        .flatMap(decodeFrom)
    }
  }

  /** Data entity containing a encryption algorithm identifier and a private key for that algorithm */
  trait EncryptionPrivateKey {
    private[Encryption] val `type`: encryptionAlgorithmsCollection.EncryptionAlgorithmType
    private[Encryption] val lowlevelKey: `type`.algorithm.PrivateKey

    /** Encodes this key, including the algorithm identifier, into a ByteString */
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

    /** Tries to restore a `EncryptionPrivateKey` from the content of the `bytes` ByteString */
    def decodeFrom(bytes: ByteString): Either[EncryptionPrivateKeyDecodeError, EncryptionPrivateKey] = {
      TypedByteString
        .decodeFrom(bytes)
        .left
        .map(e => EncryptionPrivateKeyDecodeError.DataExtractionError(e))
        .flatMap(decodeFrom)
    }
  }

  /** Contains a `public` encryption key, and it's `private` counterpart */
  case class EncryptionKeyPair(public: EncryptionPublicKey, `private`: EncryptionPrivateKey)

  /** Data entity containing some encrypted data and the identifier of the encryption algorithm used to generate it */
  class EncryptedData(
      private[Encryption] val `type`: encryptionAlgorithmsCollection.EncryptionAlgorithmType,
      private[Encryption] val bytes: EncryptedBytes) {

    /** Encodes this encrypted data, including the algorithm identifier, into a ByteString */
    def toByteString: ByteString =
      EncryptedData.encodeInto(this).toByteString
  }

  object EncryptedData {

    private[Encryption] def apply(tpe: encryptionAlgorithmsCollection.EncryptionAlgorithmType, bytes: EncryptedBytes): EncryptedData =
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

    /** Tries to restore an `EncryptedData` from the content of the `bytes` ByteString */
    def decodeFrom(bytes: ByteString): Either[EncryptedDataDecodeError, EncryptedData] = {
      TypedByteString
        .decodeFrom(bytes)
        .left
        .map(e => EncryptedDataDecodeError.DataExtractionError(e))
        .flatMap(decodeFrom)
    }
  }

  /**
    * ADT describing the types of error that can happen when trying to decrypt an entity
    */
  sealed trait DecryptError
  object DecryptError {

    /** An error has ocurred trying to decrypt a string of bytes in the underlaying algorithm */
    case class UnderlayingDecryptionError(underlaying: encryption.DecryptError) extends DecryptError

    /** The encryption key and the encrypted data were NOT generated by the same underlaying algorithm */
    case class IncompatibleEncryptionAlgorithm(keyAlgorithm: String, dataAlgorithm: String) extends DecryptError

    /** The Decoder has not been able to extract the entity from the decrypted bytes */
    case object EntityCouldNotBeDecoded extends DecryptError
  }

  /**
    * ADT describing the types of error that can happen when trying to decode a public encryption key
    * from a ByteString
    */
  sealed trait EncryptionPublicKeyDecodeError
  object EncryptionPublicKeyDecodeError {

    /** Missing or wrong information in the ByteString */
    case class DataExtractionError(cause: TypedByteStringDecodingError) extends EncryptionPublicKeyDecodeError

    /**
      * The `algorithmIdentifier` identifier recovered from the ByteString does not match any algorithm
      * supported by the `crypto` package
      */
    case class UnsupportedAlgorithm(algorithmIdentifier: String) extends EncryptionPublicKeyDecodeError

    /**
      * The underlaying algorithm has not been able to convert the `bytes` into an actual `key`
      */
    case class KeyDecodingError(cause: io.iohk.cef.crypto.KeyDecodingError) extends EncryptionPublicKeyDecodeError
  }


  /**
    * ADT describing the types of error that can happen when trying to decode a private encryption key
    * from a ByteString
    */
  sealed trait EncryptionPrivateKeyDecodeError
  object EncryptionPrivateKeyDecodeError {

    /** Missing or wrong information in the ByteString */
    case class DataExtractionError(cause: TypedByteStringDecodingError) extends EncryptionPrivateKeyDecodeError

    /**
      * The `algorithmIdentifier` identifier recovered from the ByteString does not match any algorithm
      * supported by the `crypto` package
      */
    case class UnsupportedAlgorithm(algorithmIdentifier: String) extends EncryptionPrivateKeyDecodeError

    /**
      * The underlaying algorithm has not been able to convert the `bytes` into an actual `key`
      */
    case class KeyDecodingError(cause: io.iohk.cef.crypto.KeyDecodingError) extends EncryptionPrivateKeyDecodeError
  }

  /**
    * ADT describing the types of error that can happen when trying to decode an entity of type `EncryptedData`
    * from a ByteString
    */
  sealed trait EncryptedDataDecodeError
  object EncryptedDataDecodeError {

    /** Missing or wrong information in the ByteString */
    case class DataExtractionError(cause: TypedByteStringDecodingError) extends EncryptedDataDecodeError

    /**
      * The `algorithmIdentifier` identifier recovered from the ByteString does not match any algorithm
      * supported by the `crypto` package
      */
    case class UnsupportedAlgorithm(algorithmIdentifier: String) extends EncryptedDataDecodeError
  }

}
