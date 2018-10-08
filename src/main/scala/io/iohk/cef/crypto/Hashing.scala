package io.iohk.cef.crypto

import akka.util.ByteString
import io.iohk.cef.crypto.hashing.HashingAlgorithmsCollection
import io.iohk.cef.crypto.hashing.HashBytes
import io.iohk.cef.crypto.encoding.TypedByteString
import io.iohk.cef.crypto.encoding.TypedByteStringDecodingError
import io.iohk.cef.utils._

/**
  * This is just an example on how sbt-doctest works. It's to be replaced with proper docs on CE-273
  * {{{
  *
  * >>> import akka.util.ByteString
  * >>> Hash.decodeFrom(ByteString("ABCD"))
  * Left(DataExtractionError(NioDecoderFailedToDecodeTBS))
  *
  * }}}
  */
trait Hashing {

  // PARAMETERS
  protected val hashingCollection: HashingAlgorithmsCollection
  protected val hashingType: hashingCollection.HashingAlgorithmType

  /**
    * Generates a hash for `entity`, using the default hashing algorithm
    *
    * @tparam T        the type of `entity`
    *
    * @param  entity   the entity that needs to be hashed
    * @param  encoder  how to convert `entity` into a stream of bytes
    *
    * @return          a hash of `entity`
    */
  def hash[T](entity: T)(implicit encoder: CryptoEncoder[T]): Hash =
    new Hash(hashingType, hashingType.algorithm.hash(encoder.encode(entity)))

  /**
    * Returns `true` if `hash` is a hash of `entity`, when using the hashing algorithm
    * encoded in `hash`
    *
    * @tparam T          the type of `entity`
    *
    * @param  entity     the entity that needs to be checked
    * @param  hash       the hash that needs to be checked. It also identifies the
    *                    hashing algorithm to use
    * @param  encoder    how to convert `entity` into a stream of bytes
    *
    * @return            `true` if `hash` is a valid hash of `entity`
    */
  def isValidHash[T](entity: T, hash: Hash)(implicit encoder: CryptoEncoder[T]): Boolean =
    hash.`type`.algorithm.hash(encoder.encode(entity)) == hash.bytes

  /** Data entity containing a hash and the identifier of the hashing algorithm used to generate it */
  class Hash(
      private[Hashing] val `type`: hashingCollection.HashingAlgorithmType,
      private[Hashing] val bytes: HashBytes) {

    /** Encodes this hash, including the algorithm identifier, into a ByteString */
    def toByteString: ByteString =
      Hash.encodeInto(this).toByteString

    override def equals(obj: scala.Any): Boolean = obj match {
      case that: Hash => `type` == that.`type` && bytes == that.bytes
      case _ => false
    }

    override def toString(): String =
      toByteString.hexDump
  }

  object Hash {

    private[Hashing] def encodeInto(hash: Hash): TypedByteString =
      TypedByteString(hash.`type`.algorithmIdentifier, hash.bytes.bytes)

    private[Hashing] def decodeFrom(tbs: TypedByteString): Either[HashDecodeError, Hash] = {
      hashingCollection(tbs.`type`) match {
        case Some(hashingType) =>
          Right(new Hash(hashingType, HashBytes(tbs.bytes)))
        case None =>
          Left(HashDecodeError.UnsupportedAlgorithm(tbs.`type`))
      }
    }

    /** Tries to restore a `Hash` from the content of the `bytes` ByteString */
    def decodeFrom(bytes: ByteString): Either[HashDecodeError, Hash] = {
      TypedByteString
        .decodeFrom(bytes)
        .left
        .map(e => HashDecodeError.DataExtractionError(e))
        .flatMap(decodeFrom)
    }
  }

  /**
    * ADT describing the types of error that can happen when trying to decode a hash
    * from a ByteString
    */
  sealed trait HashDecodeError
  object HashDecodeError {

    /** Missing or wrong information in the ByteString */
    case class DataExtractionError(cause: TypedByteStringDecodingError) extends HashDecodeError

    /**
      * The `algorithmIdentifier` identifier recovered from the ByteString does not match any algorithm
      * supported by the `crypto` package
      */
    case class UnsupportedAlgorithm(algorithmIdentifier: String) extends HashDecodeError
  }

}
