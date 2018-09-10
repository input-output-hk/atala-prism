package io.iohk.cef.crypto

import akka.util.ByteString
import io.iohk.cef.crypto.hashing.HashingAlgorithmsCollection
import io.iohk.cef.crypto.hashing.HashBytes
import io.iohk.cef.crypto.encoding.TypedByteString
import io.iohk.cef.crypto.encoding.TypedByteStringDecodingError

trait Hashing {

  // PARAMETERS
  protected val hashingCollection: HashingAlgorithmsCollection
  protected val hashingType: hashingCollection.HashingAlgorithmType

  def hashBytes(bytes: ByteString): Hash = new Hash(hashingType, hashingType.algorithm.hash(bytes))
  def hashEntity[T](entity: T)(implicit encoder: Encoder[T]): Hash = hashBytes(encoder.encode(entity))
  def isValidHash(bytes: ByteString, hash: Hash): Boolean =
    hash.`type`.algorithm.hash(bytes) == hash.bytes
  def isValidHash[T](entity: T, hash: Hash)(implicit encoder: Encoder[T]): Boolean =
    isValidHash(encoder.encode(entity), hash)

  class Hash(
      private[Hashing] val `type`: hashingCollection.HashingAlgorithmType,
      private[Hashing] val bytes: HashBytes) {
    def toByteString: ByteString =
      Hash.encodeInto(this).toByteString
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
