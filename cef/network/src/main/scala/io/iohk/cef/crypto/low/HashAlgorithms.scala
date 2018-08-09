package io.iohk.cef.crypto.low

import akka.util.ByteString
import org.bouncycastle.crypto.digests.KeccakDigest

trait HashAlgorithms {

  sealed trait HashAlgorithm {

    def apply(input: ByteString): ByteString

  }

  sealed trait ArrayBasedHashAlgorithm extends HashAlgorithm {
    protected def apply(input: Array[Byte]): Array[Byte]

    override final def apply(input: ByteString): ByteString =
      ByteString(apply(input.toArray))
  }

  object HashAlgorithm {

    case object KEC256 extends ArrayBasedHashAlgorithm {
      override final protected def apply(input: Array[Byte]): Array[Byte] = {
        val digest = new KeccakDigest(256)
        val output = Array.ofDim[Byte](digest.getDigestSize)
        digest.update(input, 0, input.length)
        digest.doFinal(output, 0)
        output
      }
    }

  }

}

