package io.iohk.cef.crypto
package hashing
package algorithms

import java.security.MessageDigest

private[crypto] case object SHA256 extends ArrayBasedHashAlgorithm {

  override final protected def hash(source: Array[Byte]): Array[Byte] = {
    // TODO: This is unsafe on huge inputs
    MessageDigest
      .getInstance("SHA-256")
      .digest(source)
  }

}
