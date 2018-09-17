package io.iohk.cef.crypto

import akka.util.ByteString
import io.iohk.cef.crypto.encoding.{TypedByteString, TypedByteStringDecodingError}
import io.iohk.cef.crypto.signing.{SigningAlgorithmsCollection, _}

trait Signing {

//  // PARAMETERS

  protected val signingAlgorithmsCollection: SigningAlgorithmsCollection
  protected val defaultSigningType: signingAlgorithmsCollection.SigningAlgorithmType

  def generateSigningKeyPair(): SigningKeyPair = {
    val (llPub, llPriv) = defaultSigningType.algorithm.generateKeyPair()
    SigningKeyPair(SigningPublicKey(defaultSigningType)(llPub), SigningPrivateKey(defaultSigningType)(llPriv))
  }

  def signBytes(bytes: ByteString, key: SigningPrivateKey): Signature = {
    val signature = key.`type`.algorithm.sign(bytes, key.lowlevelKey)
    Signature(key.`type`, signature)
  }

  def signEntity[T](t: T, key: SigningPrivateKey)(implicit encoder: Encoder[T]): Signature =
    signBytes(encoder.encode(t), key)

  def isValidSignatureOfBytes(bytes: ByteString, signature: Signature, key: SigningPublicKey): Boolean =
    if (key.`type` != signature.`type`)
      false
    else
      key.`type`.algorithm.isSignatureValid(signature.bytes, bytes, key.lowlevelKey)

  def isValidSignature[T](t: T, signature: Signature, key: SigningPublicKey)(implicit encoder: Encoder[T]): Boolean =
    isValidSignatureOfBytes(encoder.encode(t), signature, key)

  trait SigningPublicKey {

    private[Signing] val `type`: signingAlgorithmsCollection.SigningAlgorithmType

    private[Signing] val lowlevelKey: `type`.algorithm.PublicKey

    lazy val toByteString: ByteString =
      SigningPublicKey.encodeInto(this).toByteString

    override def equals(obj: scala.Any): Boolean = obj match {
      case that: SigningPublicKey =>
        this.toByteString == that.toByteString

      case _ => false
    }

    override def hashCode(): Int = this.toByteString.hashCode()
  }

  object SigningPublicKey {

    private[Signing] def apply(tpe: signingAlgorithmsCollection.SigningAlgorithmType)(llk: tpe.algorithm.PublicKey) =
      new SigningPublicKey {
        override private[Signing] val `type`: signingAlgorithmsCollection.SigningAlgorithmType =
          tpe
        override private[Signing] val lowlevelKey: `type`.algorithm.PublicKey =
          // This is the only way to explain the compiler that `type` and `tpe` are the same thing
          llk.asInstanceOf[`type`.algorithm.PublicKey]
      }

    private[Signing] def encodeInto(key: SigningPublicKey): TypedByteString =
      TypedByteString(key.`type`.algorithmIdentifier, key.`type`.algorithm.encodePublicKey(key.lowlevelKey).bytes)

    private[Signing] def decodeFrom(tbs: TypedByteString): Either[SigningPublicKeyDecodeError, SigningPublicKey] = {
      signingAlgorithmsCollection(tbs.`type`) match {
        case Some(signingType) =>
          signingType.algorithm.decodePublicKey(PublicKeyBytes(tbs.bytes)) match {
            case Right(lowlevelKey) =>
              Right(SigningPublicKey(signingType)(lowlevelKey))
            case Left(decodingError) =>
              Left(SigningPublicKeyDecodeError.KeyDecodingError(decodingError))
          }
        case None =>
          Left(SigningPublicKeyDecodeError.UnsupportedAlgorithm(tbs.`type`))
      }
    }

    def decodeFrom(bytes: ByteString): Either[SigningPublicKeyDecodeError, SigningPublicKey] = {
      TypedByteString
        .decodeFrom(bytes)
        .left
        .map(e => SigningPublicKeyDecodeError.DataExtractionError(e))
        .flatMap(decodeFrom)
    }
  }

  trait SigningPrivateKey {
    private[Signing] val `type`: signingAlgorithmsCollection.SigningAlgorithmType
    private[Signing] val lowlevelKey: `type`.algorithm.PrivateKey
    def toByteString: ByteString =
      SigningPrivateKey.encodeInto(this).toByteString
  }

  object SigningPrivateKey {

    private[Signing] def apply(tpe: signingAlgorithmsCollection.SigningAlgorithmType)(llk: tpe.algorithm.PrivateKey) =
      new SigningPrivateKey {
        override private[Signing] val `type`: signingAlgorithmsCollection.SigningAlgorithmType =
          tpe
        override private[Signing] val lowlevelKey: `type`.algorithm.PrivateKey =
          // This is the only way to explain the compiler that `type` and `tpe` are the same thing
          llk.asInstanceOf[`type`.algorithm.PrivateKey]
      }

    private[Signing] def encodeInto(key: SigningPrivateKey): TypedByteString =
      TypedByteString(key.`type`.algorithmIdentifier, key.`type`.algorithm.encodePrivateKey(key.lowlevelKey).bytes)

    private[Signing] def decodeFrom(tbs: TypedByteString): Either[SigningPrivateKeyDecodeError, SigningPrivateKey] = {
      signingAlgorithmsCollection(tbs.`type`) match {
        case Some(signingType) =>
          signingType.algorithm.decodePrivateKey(PrivateKeyBytes(tbs.bytes)) match {
            case Right(lowlevelKey) =>
              Right(SigningPrivateKey(signingType)(lowlevelKey))
            case Left(decodingError) =>
              Left(SigningPrivateKeyDecodeError.KeyDecodingError(decodingError))
          }
        case None =>
          Left(SigningPrivateKeyDecodeError.UnsupportedAlgorithm(tbs.`type`))
      }
    }

    def decodeFrom(bytes: ByteString): Either[SigningPrivateKeyDecodeError, SigningPrivateKey] = {
      TypedByteString
        .decodeFrom(bytes)
        .left
        .map(e => SigningPrivateKeyDecodeError.DataExtractionError(e))
        .flatMap(decodeFrom)
    }
  }

  class Signature(
      private[Signing] val `type`: signingAlgorithmsCollection.SigningAlgorithmType,
      private[Signing] val bytes: SignatureBytes) {

    lazy val toByteString: ByteString =
      Signature.encodeInto(this).toByteString

    override def equals(obj: scala.Any): Boolean = obj match {
      case that: Signature =>
        this.`type` == that.`type` &&
          this.bytes == that.bytes

      case _ => false
    }

    override def hashCode(): Int = (`type`, bytes).hashCode()
  }

  object Signature {

    def apply(tpe: signingAlgorithmsCollection.SigningAlgorithmType, bytes: SignatureBytes): Signature =
      new Signature(tpe, bytes)

    private[Signing] def encodeInto(signature: Signature): TypedByteString =
      TypedByteString(signature.`type`.algorithmIdentifier, signature.bytes.bytes)

    private[Signing] def decodeFrom(tbs: TypedByteString): Either[SignatureDecodeError, Signature] = {
      signingAlgorithmsCollection(tbs.`type`) match {
        case Some(signingType) =>
          Right(new Signature(signingType, SignatureBytes(tbs.bytes)))
        case None =>
          Left(SignatureDecodeError.UnsupportedAlgorithm(tbs.`type`))
      }
    }

    def decodeFrom(bytes: ByteString): Either[SignatureDecodeError, Signature] = {
      TypedByteString
        .decodeFrom(bytes)
        .left
        .map(e => SignatureDecodeError.DataExtractionError(e))
        .flatMap(decodeFrom)
    }
  }

  case class SigningKeyPair(public: SigningPublicKey, `private`: SigningPrivateKey)

  sealed trait SigningPublicKeyDecodeError
  object SigningPublicKeyDecodeError {
    case class DataExtractionError(cause: TypedByteStringDecodingError) extends SigningPublicKeyDecodeError
    case class UnsupportedAlgorithm(algorithmIdentifier: String) extends SigningPublicKeyDecodeError
    case class KeyDecodingError(cause: io.iohk.cef.crypto.KeyDecodingError) extends SigningPublicKeyDecodeError
  }

  sealed trait SigningPrivateKeyDecodeError
  object SigningPrivateKeyDecodeError {
    case class DataExtractionError(cause: TypedByteStringDecodingError) extends SigningPrivateKeyDecodeError
    case class UnsupportedAlgorithm(algorithmIdentifier: String) extends SigningPrivateKeyDecodeError
    case class KeyDecodingError(cause: io.iohk.cef.crypto.KeyDecodingError) extends SigningPrivateKeyDecodeError
  }

  sealed trait SignatureDecodeError
  object SignatureDecodeError {
    case class DataExtractionError(cause: TypedByteStringDecodingError) extends SignatureDecodeError
    case class UnsupportedAlgorithm(algorithmIdentifier: String) extends SignatureDecodeError
  }

}
