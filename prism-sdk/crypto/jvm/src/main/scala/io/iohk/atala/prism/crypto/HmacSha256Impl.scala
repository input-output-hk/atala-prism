package io.iohk.atala.prism.crypto

import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.digests.Sha256Digest
import org.bouncycastle.crypto.params.KeyParameter

/**
  * HMAC-SHA-256 Java implementation using Bouncy Castle.
  */
private[crypto] object HmacSha256Impl {

  /**
    * Compute HMAC-SHA-256 data authentication code using shared key.
    */
  def compute(data: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val hmac = new HMac(new Sha256Digest)
    hmac.init(new KeyParameter(key))
    hmac.update(data, 0, data.size)
    val out = new Array[Byte](32)
    hmac.doFinal(out, 0)
    out
  }
}
