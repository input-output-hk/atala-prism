package io.iohk.cef.crypto
package hashing
package algorithms

import org.bouncycastle.crypto.digests.KeccakDigest

private[crypto] case object KECCAK256 extends ArrayBasedHashAlgorithm {

  override final protected def hash(source: Array[Byte]): Array[Byte] = {
    val digest = new KeccakDigest(256)
    val output = Array.ofDim[Byte](digest.getDigestSize)

    // TODO: This is unsafe on huge inputs
    digest.update(source, 0, source.length)
    digest.doFinal(output, 0)
    output
  }

}
