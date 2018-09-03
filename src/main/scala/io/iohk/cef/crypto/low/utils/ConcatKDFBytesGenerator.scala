package io.iohk.cef.crypto.low
package utils

import akka.util.ByteString
import org.bouncycastle.crypto.Digest
import org.bouncycastle.util.Pack

/**
  * Basic KDF generator for derived keys and ivs as defined by NIST SP 800-56A.
  * @param digest for source of derived keys
  */
private[utils] class ConcatKDFBytesGenerator(digest: Digest) {
  val digestSize: Int = digest.getDigestSize

  /**
    *
    * @param outputLength length of output that will be produced by this method,
    *                     maximum value is (digest output size in bits) * (2^32 - 1) but it should not be a problem
    *                     because we are using Int
    * @throws scala.IllegalArgumentException ("Output length too large") when outputLength is greater than (digest output size in bits) * (2^32 - 1)
    * @return returns bytes generated by key derivation function
    */
  @throws[IllegalArgumentException]
  def generateBytes(outputLength: Int, seed: Array[Byte]): ByteString = {
    require(outputLength <= (digestSize * 8) * ((2L << 32) - 1), "Output length too large")

    val counterStart: Long = 1
    val hashBuf = new Array[Byte](digestSize)
    val counterValue = new Array[Byte](Integer.BYTES)

    digest.reset()

    (0 until (outputLength / digestSize + 1))
      .map { i =>
        Pack.intToBigEndian(((counterStart + i) % (2L << 32)).toInt, counterValue, 0)
        digest.update(counterValue, 0, counterValue.length)
        digest.update(seed, 0, seed.length)
        digest.doFinal(hashBuf, 0)

        val spaceLeft = outputLength - (i * digestSize)

        if (spaceLeft > digestSize) {
          ByteString(hashBuf)
        } else {
          ByteString(hashBuf).dropRight(digestSize - spaceLeft)
        }
      }
      .reduce[ByteString] { case (a, b) => a ++ b }
  }
}
