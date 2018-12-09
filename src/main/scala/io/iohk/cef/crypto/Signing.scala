package io.iohk.cef.crypto

import akka.util.ByteString
import io.iohk.cef.crypto.encoding.TypedByteString
import io.iohk.cef.crypto.signing.{SigningAlgorithmsCollection, _}
import io.iohk.cef.codecs.nio._
import io.iohk.cef.utils._

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
    * @param  codec  how to convert `t` into a stream of bytes
    *
    * @return          a signature of `t`
    */
  def sign[T](t: T, key: SigningPrivateKey)(implicit codec: NioCodec[T]): Signature = {
    val signature = key.`type`.algorithm.sign(codec.encode(t).toByteString, key.lowlevelKey)
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
  def isValidSignature[T](t: T, signature: Signature, key: SigningPublicKey)(implicit encoder: NioCodec[T]): Boolean =
    if (key.`type` != signature.`type`)
      false
    else
      key.`type`.algorithm.isSignatureValid(signature.bytes, encoder.encode(t).toByteString, key.lowlevelKey)

  def toSigningPublicKey(obj: AnyRef): Option[SigningPublicKey] = {
    for {
      tpe <- signingAlgorithmsCollection.from(obj)
      key <- tpe.algorithm.toPublicKey(obj)
    } yield SigningPublicKey.apply(tpe)(key)
  }

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

    override def toString(): String = SigningPublicKey.show(this)
  }

  object SigningPublicKey extends KeyEntityCompanion[SigningPublicKey] {

    override protected val title: String = "SIGNING PUBLIC KEY"

    private[crypto] def apply(tpe: signingAlgorithmsCollection.SigningAlgorithmType)(llk: tpe.algorithm.PublicKey) =
      new SigningPublicKey {
        override private[Signing] val `type`: signingAlgorithmsCollection.SigningAlgorithmType =
          tpe
        override private[Signing] val lowlevelKey: `type`.algorithm.PublicKey =
          // This is the only way to explain the compiler that `type` and `tpe` are the same thing
          llk.asInstanceOf[`type`.algorithm.PublicKey]
      }

    override private[crypto] def encodeInto(key: SigningPublicKey): TypedByteString =
      TypedByteString(key.`type`.algorithmIdentifier, key.`type`.algorithm.encodePublicKey(key.lowlevelKey).bytes)

    override private[crypto] def decodeFrom(
        tbs: TypedByteString): Either[KeyDecodeError[SigningPublicKey], SigningPublicKey] = {
      signingAlgorithmsCollection(tbs.`type`) match {
        case Some(signingType) =>
          signingType.algorithm.decodePublicKey(PublicKeyBytes(tbs.bytes)) match {
            case Right(lowlevelKey) =>
              Right(SigningPublicKey(signingType)(lowlevelKey))
            case Left(decodingError) =>
              Left(KeyDecodeError.KeyDecodingError[SigningPublicKey](decodingError))
          }
        case None =>
          Left(KeyDecodeError.UnsupportedAlgorithm[SigningPublicKey](tbs.`type`))
      }
    }
  }

  /** Data entity containing a signing algorithm identifier and a private key for that algorithm */
  trait SigningPrivateKey {
    private[Signing] val `type`: signingAlgorithmsCollection.SigningAlgorithmType
    private[Signing] val lowlevelKey: `type`.algorithm.PrivateKey

    /** Encodes this key, including the algorithm identifier, into a ByteString */
    def toByteString: ByteString =
      SigningPrivateKey.encodeInto(this).toByteString

    override def equals(obj: scala.Any): Boolean = obj match {
      case that: SigningPrivateKey =>
        this.toByteString == that.toByteString

      case _ => false
    }

    override def hashCode(): Int = this.toByteString.hashCode()

    override def toString(): String = SigningPrivateKey.show(this)
  }

  object SigningPrivateKey extends KeyEntityCompanion[SigningPrivateKey] {

    override protected val title: String = "SIGNING PRIVATE KEY"

    private[Signing] def apply(tpe: signingAlgorithmsCollection.SigningAlgorithmType)(llk: tpe.algorithm.PrivateKey) =
      new SigningPrivateKey {
        override private[Signing] val `type`: signingAlgorithmsCollection.SigningAlgorithmType =
          tpe
        override private[Signing] val lowlevelKey: `type`.algorithm.PrivateKey =
          // This is the only way to explain the compiler that `type` and `tpe` are the same thing
          llk.asInstanceOf[`type`.algorithm.PrivateKey]
      }

    override private[crypto] def encodeInto(key: SigningPrivateKey): TypedByteString =
      TypedByteString(key.`type`.algorithmIdentifier, key.`type`.algorithm.encodePrivateKey(key.lowlevelKey).bytes)

    override private[crypto] def decodeFrom(
        tbs: TypedByteString): Either[KeyDecodeError[SigningPrivateKey], SigningPrivateKey] = {
      signingAlgorithmsCollection(tbs.`type`) match {
        case Some(signingType) =>
          signingType.algorithm.decodePrivateKey(PrivateKeyBytes(tbs.bytes)) match {
            case Right(lowlevelKey) =>
              Right(SigningPrivateKey(signingType)(lowlevelKey))
            case Left(decodingError) =>
              Left(KeyDecodeError.KeyDecodingError[SigningPrivateKey](decodingError))
          }
        case None =>
          Left(KeyDecodeError.UnsupportedAlgorithm[SigningPrivateKey](tbs.`type`))
      }
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

    override def toString(): String = Signature.show(this)
  }

  object Signature extends CryptoEntityCompanion[Signature] {

    override protected val title: String = "SIGNATURE"

    private[Signing] def apply(
        tpe: signingAlgorithmsCollection.SigningAlgorithmType,
        bytes: SignatureBytes): Signature =
      new Signature(tpe, bytes)

    override private[crypto] def encodeInto(signature: Signature): TypedByteString =
      TypedByteString(signature.`type`.algorithmIdentifier, signature.bytes.bytes)

    override private[crypto] def decodeFrom(tbs: TypedByteString): Either[DecodeError[Signature], Signature] = {
      signingAlgorithmsCollection(tbs.`type`) match {
        case Some(signingType) =>
          Right(new Signature(signingType, SignatureBytes(tbs.bytes)))
        case None =>
          Left(DecodeError.UnsupportedAlgorithm[Signature](tbs.`type`))
      }
    }
  }

  /** Contains a `public` signing key, and it's `private` counterpart */
  case class SigningKeyPair private[Signing] (public: SigningPublicKey, `private`: SigningPrivateKey) {
    override def toString(): String =
      s"$public\n${`private`}"
  }

}
