package io.iohk.cef.crypto

import akka.util.ByteString
import io.iohk.cef.crypto.encoding.{TypedByteString, TypedByteStringDecodingError}
import io.iohk.cef.crypto.signing.{SigningAlgorithmsCollection, _}

trait Signing {

  // PARAMETERS

  protected val signingAlgorithmsCollection: SigningAlgorithmsCollection
  protected val defaultSigningType: signingAlgorithmsCollection.SigningAlgorithmType

  /**
    * Generates a pair of signing keys, using the default signing algorithm. These
    * keys will identify, too, the signing algorithm they can work with.
    *
    * @return a pair of signing keys
    */
  def generateSigningKeyPair(): SigningKeyPair = {
    val (llPub, llPriv) = defaultSigningType.algorithm.generateKeyPair()
    SigningKeyPair(SigningPublicKey(defaultSigningType)(llPub), SigningPrivateKey(defaultSigningType)(llPriv))
  }

  /**
    * Generates a signature for `t`, using the `key` private key and the algorithm
    * supported by the `key`
    *
    * @tparam T        the type of `t`
    *
    * @param  t        the entity that needs to be signed
    * @param  key      the key to be used. It also identifies the signing algorithm
    *                  to use
    * @param  encoder  how to convert `t` into a stream of bytes
    *
    * @return          a signature of `t`
    */
  def sign[T](t: T, key: SigningPrivateKey)(implicit encoder: Encoder[T]): Signature = {
    val signature = key.`type`.algorithm.sign(encoder.encode(t), key.lowlevelKey)
    Signature(key.`type`, signature)
  }

  /**
    * Returns `true` if `signature` and `key` were generated with the same signing
    * algorithm AND if `signature` is a signature of `t`, when checked using `key`
    *
    * @tparam T          the type of `t`
    *
    * @param  t          the entity that needs to be checked
    * @param  signature  the signature that needs to be checked. It also identifies the
    *                    signing algorithm to use
    * @param  key        the key to be used. It also identifies the signing algorithm
    *                    to use
    * @param  encoder    how to convert `t` into a stream of bytes
    *
    * @return            `true` if `signature` is a valid signature of `t`
    */
  def isValidSignature[T](t: T, signature: Signature, key: SigningPublicKey)(implicit encoder: Encoder[T]): Boolean =
    if (key.`type` != signature.`type`)
      false
    else
      key.`type`.algorithm.isSignatureValid(signature.bytes, encoder.encode(t), key.lowlevelKey)

  /** Data entity containing a signing algorithm identifier and a public key for that algorithm */
  trait SigningPublicKey {

    private[Signing] val `type`: signingAlgorithmsCollection.SigningAlgorithmType

    private[Signing] val lowlevelKey: `type`.algorithm.PublicKey

    /** Encodes this key, including the algorithm identifier, into a ByteString */
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

    /** Tries to restore a `SigningPublicKey` from the content of the `bytes` ByteString */
    def decodeFrom(bytes: ByteString): Either[SigningPublicKeyDecodeError, SigningPublicKey] = {
      TypedByteString
        .decodeFrom(bytes)
        .left
        .map(e => SigningPublicKeyDecodeError.DataExtractionError(e))
        .flatMap(decodeFrom)
    }
  }

  /** Data entity containing a signing algorithm identifier and a private key for that algorithm */
  trait SigningPrivateKey {
    private[Signing] val `type`: signingAlgorithmsCollection.SigningAlgorithmType
    private[Signing] val lowlevelKey: `type`.algorithm.PrivateKey

    /** Encodes this key, including the algorithm identifier, into a ByteString */
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

    /** Tries to restore a `SigningPrivateKey` from the content of the `bytes` ByteString */
    def decodeFrom(bytes: ByteString): Either[SigningPrivateKeyDecodeError, SigningPrivateKey] = {
      TypedByteString
        .decodeFrom(bytes)
        .left
        .map(e => SigningPrivateKeyDecodeError.DataExtractionError(e))
        .flatMap(decodeFrom)
    }
  }

  /** Data entity containing a signature and the identifier of the signing algorithm used to generate it */
  class Signature(
      private[Signing] val `type`: signingAlgorithmsCollection.SigningAlgorithmType,
      private[Signing] val bytes: SignatureBytes) {

    /** Encodes this signature, including the algorithm identifier, into a ByteString */
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

    private[Signing] def apply(
        tpe: signingAlgorithmsCollection.SigningAlgorithmType,
        bytes: SignatureBytes): Signature =
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

    /** Tries to restore a `Signature` from the content of the `bytes` ByteString */
    def decodeFrom(bytes: ByteString): Either[SignatureDecodeError, Signature] = {
      TypedByteString
        .decodeFrom(bytes)
        .left
        .map(e => SignatureDecodeError.DataExtractionError(e))
        .flatMap(decodeFrom)
    }
  }

  /** Contains a `public` signing key, and it's `private` counterpart */
  case class SigningKeyPair private[Signing] (public: SigningPublicKey, `private`: SigningPrivateKey)

  /**
    * ADT describing the types of error that can happen when trying to decode a public signing key
    * from a ByteString
    */
  sealed trait SigningPublicKeyDecodeError
  object SigningPublicKeyDecodeError {

    /** Missing or wrong information in the ByteString */
    case class DataExtractionError(cause: TypedByteStringDecodingError) extends SigningPublicKeyDecodeError

    /**
      * The `algorithmIdentifier` identifier recovered from the ByteString does not match any algorithm
      * supported by the `crypto` package
      */
    case class UnsupportedAlgorithm(algorithmIdentifier: String) extends SigningPublicKeyDecodeError

    /**
      * The underlaying algorithm has not been able to convert the `bytes` into an actual `key`
      */
    case class KeyDecodingError(cause: io.iohk.cef.crypto.KeyDecodingError) extends SigningPublicKeyDecodeError
  }

  /**
    * ADT describing the types of error that can happen when trying to decode a private signing key
    * from a ByteString
    */
  sealed trait SigningPrivateKeyDecodeError
  object SigningPrivateKeyDecodeError {

    /** Missing or wrong information in the ByteString */
    case class DataExtractionError(cause: TypedByteStringDecodingError) extends SigningPrivateKeyDecodeError

    /**
      * The `algorithmIdentifier` identifier recovered from the ByteString does not match any algorithm
      * supported by the `crypto` package
      */
    case class UnsupportedAlgorithm(algorithmIdentifier: String) extends SigningPrivateKeyDecodeError

    /**
      * The underlaying algorithm has not been able to convert the `bytes` into an actual `key`
      */
    case class KeyDecodingError(cause: io.iohk.cef.crypto.KeyDecodingError) extends SigningPrivateKeyDecodeError
  }

  /**
    * ADT describing the types of error that can happen when trying to decode a signature
    * from a ByteString
    */
  sealed trait SignatureDecodeError
  object SignatureDecodeError {

    /** Missing or wrong information in the ByteString */
    case class DataExtractionError(cause: TypedByteStringDecodingError) extends SignatureDecodeError

    /**
      * The `algorithmIdentifier` identifier recovered from the ByteString does not match any algorithm
      * supported by the `crypto` package
      */
    case class UnsupportedAlgorithm(algorithmIdentifier: String) extends SignatureDecodeError
  }

}
