package io.iohk.cef

import akka.util.ByteString
import org.bouncycastle.crypto.digests.KeccakDigest

package object crypto {


  def kec256(input: Array[Byte]*): Array[Byte] = {
    val digest = new KeccakDigest(256)
    val output = Array.ofDim[Byte](digest.getDigestSize)
    input.foreach(i => digest.update(i, 0, i.length))
    digest.doFinal(output, 0)
    output
  }

  def kec256(input: ByteString): ByteString =
    ByteString(kec256(input.toArray))
}
