package io.iohk.cef.crypto

import akka.util.ByteString
import io.iohk.cef.crypto.hashing.HashingAlgorithmsCollection
import io.iohk.cef.crypto.hashing.HashBytes
import io.iohk.cef.crypto.encoding.TypedByteString
import io.iohk.cef.codecs.nio.NioEncoder
import io.iohk.cef.utils._

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
  def hash[T](entity: T)(implicit encoder: NioEncoder[T]): Hash =
    new Hash(hashingType, hashingType.algorithm.hash(encoder.encode(entity).toByteString))

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
  def isValidHash[T](entity: T, hash: Hash)(implicit encoder: NioEncoder[T]): Boolean =
    hash.`type`.algorithm.hash(encoder.encode(entity).toByteString) == hash.bytes

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

    /**
      * {{{
      *
      * >>> import akka.util.ByteString
      * >>> import io.iohk.cef.codecs.nio.auto._
      * >>> hash(ByteString("ABC"))
      * -----BEGIN HASH SHA256 BLOCK-----
      *  00 00 00 06 53 48 41 32 35 36 00 00 00 20 26 57
      *  20 F4 DF 20 FD 3D 58 9C BC C6 1A F9 52 AA 6D AA
      *  B2 18 AA E4 84 11 4C 4F 84 8D 62 A7 F0 80
      * -----END HASH SHA256 BLOCK-----
      *
      * }}}
      */
    override def toString(): String =
      Hash.show(this)
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
