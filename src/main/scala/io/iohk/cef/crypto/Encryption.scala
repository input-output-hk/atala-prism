package io.iohk.cef.crypto

import akka.util.ByteString

import io.iohk.cef.crypto.encryption._
import io.iohk.cef.crypto.encoding.TypedByteString
import io.iohk.cef.codecs.nio.NioCodec
import io.iohk.cef.utils._

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
  def encrypt[T](entity: T, key: EncryptionPublicKey)(implicit codec: NioCodec[T]): EncryptedData = {
    val encryptedBytes =
      key.`type`.algorithm.encrypt(codec.encode(entity).toByteString, key.lowlevelKey)

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
      implicit codec: NioCodec[T]): Either[DecryptError, T] = {
    decryptBytes(encryptedData, key)
      .flatMap { bytes =>
        codec.decode(bytes.toByteBuffer) match {
          case Some(e) => Right(e)
          case None => Left(DecryptError.EntityCouldNotBeDecoded)
        }
      }
  }

  /** Data entity containing a encryption algorithm identifier and a public key for that algorithm */
  trait EncryptionPublicKey extends KeyEntity[EncryptionPublicKey, EncryptionPublicKey.type] {
    private[Encryption] val `type`: encryptionAlgorithmsCollection.EncryptionAlgorithmType
    private[Encryption] val lowlevelKey: `type`.algorithm.PublicKey

    private[crypto] val companion: EncryptionPublicKey.type = EncryptionPublicKey
    protected val self: EncryptionPublicKey = this

    override def equals(obj: scala.Any): Boolean = obj match {
      case that: EncryptionPublicKey =>
        this.toByteString == that.toByteString

      case _ => false
    }

  }

  object EncryptionPublicKey extends KeyEntityCompanion[EncryptionPublicKey] {

    protected val title: String = "ENCRYPTION PUBLIC KEY"

    private[Encryption] def apply(tpe: encryptionAlgorithmsCollection.EncryptionAlgorithmType)(
        llk: tpe.algorithm.PublicKey) =
      new EncryptionPublicKey {
        override private[Encryption] val `type`: encryptionAlgorithmsCollection.EncryptionAlgorithmType =
          tpe
        override private[Encryption] val lowlevelKey: `type`.algorithm.PublicKey =
          // This is the only way to explain the compiler that `type` and `tpe` are the same thing
          llk.asInstanceOf[`type`.algorithm.PublicKey]
      }

    private[crypto] def encodeInto(key: EncryptionPublicKey): TypedByteString =
      TypedByteString(key.`type`.algorithmIdentifier, key.`type`.algorithm.encodePublicKey(key.lowlevelKey).bytes)

    private[crypto] def decodeFrom(
        tbs: TypedByteString): Either[KeyDecodeError[EncryptionPublicKey], EncryptionPublicKey] = {
      encryptionAlgorithmsCollection(tbs.`type`) match {
        case Some(encryptionType) =>
          encryptionType.algorithm.decodePublicKey(PublicKeyBytes(tbs.bytes)) match {
            case Right(lowlevelKey) =>
              Right(EncryptionPublicKey(encryptionType)(lowlevelKey))
            case Left(decodingError) =>
              Left(KeyDecodeError.KeyDecodingError[EncryptionPublicKey](decodingError))
          }
        case None =>
          Left(KeyDecodeError.UnsupportedAlgorithm[EncryptionPublicKey](tbs.`type`))
      }
    }

  }

  /** Data entity containing a encryption algorithm identifier and a private key for that algorithm */
  trait EncryptionPrivateKey extends KeyEntity[EncryptionPrivateKey, EncryptionPrivateKey.type] {
    private[Encryption] val `type`: encryptionAlgorithmsCollection.EncryptionAlgorithmType
    private[Encryption] val lowlevelKey: `type`.algorithm.PrivateKey

    private[crypto] val companion: EncryptionPrivateKey.type = EncryptionPrivateKey
    protected val self: EncryptionPrivateKey = this

    override def equals(obj: scala.Any): Boolean = obj match {
      case that: EncryptionPrivateKey =>
        this.toByteString == that.toByteString

      case _ => false
    }
  }

  object EncryptionPrivateKey extends KeyEntityCompanion[EncryptionPrivateKey] {

    protected val title: String = "ENCRYPTION PRIVATE KEY"

    private[Encryption] def apply(tpe: encryptionAlgorithmsCollection.EncryptionAlgorithmType)(
        llk: tpe.algorithm.PrivateKey) =
      new EncryptionPrivateKey {
        override private[Encryption] val `type`: encryptionAlgorithmsCollection.EncryptionAlgorithmType =
          tpe
        override private[Encryption] val lowlevelKey: `type`.algorithm.PrivateKey =
          // This is the only way to explain the compiler that `type` and `tpe` are the same thing
          llk.asInstanceOf[`type`.algorithm.PrivateKey]
      }

    private[crypto] def encodeInto(key: EncryptionPrivateKey): TypedByteString =
      TypedByteString(key.`type`.algorithmIdentifier, key.`type`.algorithm.encodePrivateKey(key.lowlevelKey).bytes)

    private[crypto] def decodeFrom(
        tbs: TypedByteString): Either[KeyDecodeError[EncryptionPrivateKey], EncryptionPrivateKey] = {
      encryptionAlgorithmsCollection(tbs.`type`) match {
        case Some(encryptionType) =>
          encryptionType.algorithm.decodePrivateKey(PrivateKeyBytes(tbs.bytes)) match {
            case Right(lowlevelKey) =>
              Right(EncryptionPrivateKey(encryptionType)(lowlevelKey))
            case Left(decodingError) =>
              Left(KeyDecodeError.KeyDecodingError[EncryptionPrivateKey](decodingError))
          }
        case None =>
          Left(KeyDecodeError.UnsupportedAlgorithm[EncryptionPrivateKey](tbs.`type`))
      }
    }

  }

  /** Contains a `public` encryption key, and it's `private` counterpart */
  case class EncryptionKeyPair(public: EncryptionPublicKey, `private`: EncryptionPrivateKey)

  /** Data entity containing some encrypted data and the identifier of the encryption algorithm used to generate it */
  class EncryptedData(
      private[Encryption] val `type`: encryptionAlgorithmsCollection.EncryptionAlgorithmType,
      private[Encryption] val bytes: EncryptedBytes)
      extends CryptoEntity[EncryptedData, EncryptedData.type] {

    private[crypto] val companion: EncryptedData.type = EncryptedData
    protected val self: EncryptedData = this

    override def toString(): String =
      EncryptedData.show(this)

    override def equals(obj: scala.Any): Boolean = obj match {
      case that: EncryptedData =>
        this.`type` == that.`type` && this.bytes == that.bytes

      case _ => false
    }
  }

  object EncryptedData extends CryptoEntityCompanion[EncryptedData] {

    protected val title: String = "ENCRYPTED DATA"

    private[Encryption] def apply(
        tpe: encryptionAlgorithmsCollection.EncryptionAlgorithmType,
        bytes: EncryptedBytes): EncryptedData =
      new EncryptedData(tpe, bytes)

    private[crypto] def encodeInto(signature: EncryptedData): TypedByteString =
      TypedByteString(signature.`type`.algorithmIdentifier, signature.bytes.bytes)

    private[crypto] def decodeFrom(tbs: TypedByteString): Either[DecodeError[EncryptedData], EncryptedData] = {
      encryptionAlgorithmsCollection(tbs.`type`) match {
        case Some(encryptionType) =>
          Right(new EncryptedData(encryptionType, EncryptedBytes(tbs.bytes)))
        case None =>
          Left(DecodeError.UnsupportedAlgorithm[EncryptedData](tbs.`type`))
      }
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

}
