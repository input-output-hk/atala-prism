package io.iohk.atala.crypto

import java.security.MessageDigest

private[crypto] object SHA256DigestImpl {
  private def messageDigest = MessageDigest.getInstance("SHA-256")

  def compute(data: Array[Byte]): Array[Byte] = {
    messageDigest.digest(data)
  }
}
