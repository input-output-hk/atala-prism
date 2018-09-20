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

  def hash[T](entity: T)(implicit encoder: Encoder[T]): Hash =
    new Hash(hashingType, hashingType.algorithm.hash(encoder.encode(entity)))

  def isValidHash(bytes: ByteString, hash: Hash): Boolean =
    hash.`type`.algorithm.hash(bytes) == hash.bytes

  def isValidHash[T](entity: T, hash: Hash)(implicit encoder: Encoder[T]): Boolean =
    hash.`type`.algorithm.hash(encoder.encode(entity)) == hash.bytes

  class Hash(
      private[Hashing] val `type`: hashingCollection.HashingAlgorithmType,
      private[Hashing] val bytes: HashBytes) {
    def toByteString: ByteString =
      Hash.encodeInto(this).toByteString

    override def equals(obj: scala.Any): Boolean = obj match {
      case that: Hash => `type` == that.`type` && bytes == that.bytes
      case _ => false
    }

    override def toString(): String =
      toByteString.toHex
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

    def decodeFrom(bytes: ByteString): Either[HashDecodeError, Hash] = {
      TypedByteString
        .decodeFrom(bytes)
        .left
        .map(e => HashDecodeError.DataExtractionError(e))
        .flatMap(decodeFrom)
    }
  }

  sealed trait HashDecodeError
  object HashDecodeError {
    case class DataExtractionError(cause: TypedByteStringDecodingError) extends HashDecodeError
    case class UnsupportedAlgorithm(algorithmIdentifier: String) extends HashDecodeError
  }

}
