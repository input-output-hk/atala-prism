package io.iohk.cef.crypto.low

import java.security.Security

import akka.util.ByteString
import org.bouncycastle.crypto.digests.KeccakDigest
import org.bouncycastle.jce.provider.BouncyCastleProvider

/**
  * Contract all hashing algorithm implementations should follow
  */
sealed trait HashAlgorithm {

  /**
    * Hash the provided `source` bytes
    *
    * @param source the bytes to hash
    *
    * @return a hashed version of the `source` bytes
    */
  def hash(source: ByteString): ByteString

}

/**
  * Helper trait that allows the implementation of a `HashAlgorithm` basing it on
  * `Array[Byte]` instead of `ByteString`
  */
sealed trait ArrayBasedHashAlgorithm extends HashAlgorithm {

  /**
    * Hash the provided `source` bytes
    *
    * @param source the bytes to hash
    *
    * @return a hashed version of the `source` bytes
    */
  protected def hash(source: Array[Byte]): Array[Byte]

  /** @inheritdoc */
  override final def hash(source: ByteString): ByteString =
    ByteString(hash(source.toArray))
}

/**
  * Companion object to HashAlgorithm, containing all the implemented `HashAlgorithm`
  */
object HashAlgorithm {

  Security.addProvider(new BouncyCastleProvider)

  /**
    * Implementation of the `kec256` `HashAlgorithm`
    */
  case object KEC256 extends ArrayBasedHashAlgorithm {

    /** @inheritdoc */
    override final protected def hash(source: Array[Byte]): Array[Byte] = {
      val digest = new KeccakDigest(256)
      val output = Array.ofDim[Byte](digest.getDigestSize)

      // TODO: This is unsafe on huge inputs
      digest.update(source, 0, source.length)
      digest.doFinal(output, 0)
      output
    }
  }
}
