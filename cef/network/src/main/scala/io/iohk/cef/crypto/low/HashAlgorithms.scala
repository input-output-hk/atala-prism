package io.iohk.cef.crypto.low

import akka.util.ByteString
import org.bouncycastle.crypto.digests.KeccakDigest


/**
  * Collection of hashing algorithms
  *
  * NOTE:
  *   PARTIAL IMPLEMENTATION OF
  *   package object low
  */
trait HashAlgorithms {

  /**
    * Contract all hashing algorithm implementations should follow
    */
  sealed trait HashAlgorithm {

    /**
      * Hash the provided `source` bytes
      *
      * @param source     the bytes to be encrypted
      *
      * @return a hashed version of the `source` bytes
      */
    def apply(source: ByteString): ByteString

  }


  /**
    * Helper trait that allows the implementation of a `HashAlgorithm` basing it on
    * `Array[Byte]` instead of `ByteString`
    */
  sealed trait ArrayBasedHashAlgorithm extends HashAlgorithm {

    /**
      * Hash the provided `source` bytes
      *
      * @param source     the bytes to be encrypted
      *
      * @return a hashed version of the `source` bytes
      */
    protected def apply(source: Array[Byte]): Array[Byte]

    /** @inheritdoc */
    override final def apply(source: ByteString): ByteString =
      ByteString(apply(source.toArray))
  }


  /**
    * Companion object to HashAlgorithm, containing all the implemented `HashAlgorithm`
    */
  object HashAlgorithm {

    /**
      * Implementation of the `kec256` `HashAlgorithm`
      */
    case object KEC256 extends ArrayBasedHashAlgorithm {

      /** @inheritdoc */
      override final protected def apply(source: Array[Byte]): Array[Byte] = {
        val digest = new KeccakDigest(256)
        val output = Array.ofDim[Byte](digest.getDigestSize)
        digest.update(source, 0, source.length)
        digest.doFinal(output, 0)
        output
      }
    }

  }

}

