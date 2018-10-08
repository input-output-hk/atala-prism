package io.iohk.cef.crypto

import akka.util.ByteString
import io.iohk.cef.crypto.hashing.HashingAlgorithmsCollection
import io.iohk.cef.crypto.hashing.HashBytes
import io.iohk.cef.crypto.encoding.TypedByteString

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

    /**
      * {{{
      *
      * >>> import akka.util.ByteString
      * >>> hash(ByteString("ABC"))
      * -----BEGIN HASH SHA256 BLOCK-----
      *  00 00 00 60 00 00 00 10 A7 F1 59 E6 70 8B 88 34
      *  57 37 55 A6 91 9F 54 68 00 00 00 06 00 53 00 48
      *  00 41 00 32 00 35 00 36 00 00 00 38 00 00 00 10
      *  0E BB 60 87 0B 23 D1 57 2A 23 C3 DB 6A AD 73 54
      *  00 00 00 20 B5 D4 04 5C 3F 46 6F A9 1F E2 CC 6A
      *  BE 79 23 2A 1A 57 CD F1 04 F7 A2 6E 71 6E 0A 1E
      *  27 89 DF 78
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
