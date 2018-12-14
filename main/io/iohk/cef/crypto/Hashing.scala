package io.iohk.cef.crypto

import io.iohk.cef.crypto.hashing.HashingAlgorithmsCollection
import io.iohk.cef.crypto.hashing.HashBytes
import io.iohk.cef.crypto.encoding.TypedByteString
import io.iohk.cef.codecs.nio.NioCodec
import io.iohk.cef.utils._
import io.iohk.cef.crypto.encoding._

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
    * @param  codec  how to convert `entity` into a stream of bytes
    *
    * @return          a hash of `entity`
    */
  def hash[T](entity: T)(implicit codec: NioCodec[T]): Hash =
    new Hash(hashingType, hashingType.algorithm.hash(codec.encode(entity).toByteString))

  /**
    * Returns `true` if `hash` is a hash of `entity`, when using the hashing algorithm
    * encoded in `hash`
    *
    * @tparam T          the type of `entity`
    *
    * @param  entity     the entity that needs to be checked
    * @param  hash       the hash that needs to be checked. It also identifies the
    *                    hashing algorithm to use
    * @param  codec    how to convert `entity` into a stream of bytes
    *
    * @return            `true` if `hash` is a valid hash of `entity`
    */
  def isValidHash[T](entity: T, hash: Hash)(implicit codec: NioCodec[T]): Boolean =
    hash.`type`.algorithm.hash(codec.encode(entity).toByteString) == hash.bytes

  /** Data entity containing a hash and the identifier of the hashing algorithm used to generate it */
  class Hash(private[Hashing] val `type`: hashingCollection.HashingAlgorithmType, private[Hashing] val bytes: HashBytes)
      extends CryptoEntity[Hash, Hash.type] {

    private[crypto] val companion: Hash.type = Hash
    protected val self: Hash = this

    override def equals(obj: scala.Any): Boolean = obj match {
      case that: Hash => `type` == that.`type` && bytes == that.bytes
      case _ => false
    }
  }

  object Hash extends CryptoEntityCompanion[Hash] {

    override protected val title: String = "HASH"

    private[crypto] def encodeInto(hash: Hash): TypedByteString =
      TypedByteString(hash.`type`.algorithmIdentifier, hash.bytes.bytes)

    private[crypto] def decodeFrom(tbs: TypedByteString): Either[DecodeError[Hash], Hash] = {
      hashingCollection(tbs.`type`) match {
        case Some(hashingType) =>
          Right(new Hash(hashingType, HashBytes(tbs.bytes)))
        case None =>
          Left(DecodeError.UnsupportedAlgorithm(tbs.`type`))
      }
    }
  }

}
