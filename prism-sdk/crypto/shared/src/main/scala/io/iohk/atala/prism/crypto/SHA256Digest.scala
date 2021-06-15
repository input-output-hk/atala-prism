package io.iohk.atala.prism.crypto

import io.iohk.atala.prism.util.BytesOps

final class SHA256Digest private (val value: Vector[Byte]) {
  def hexValue: String = BytesOps.bytesToHex(value)

  override def equals(obj: Any): Boolean =
    obj match {
      case that: SHA256Digest => this.value == that.value
      case _ => false
    }

  override def hashCode(): Int = value.hashCode()

  override def toString: String = s"SHA256Digest($hexValue)"
}

object SHA256Digest {
  val BYTE_LENGTH = 32
  val HEX_STRING_RE = "^(?:[0-9a-fA-F]{2})+$".r
  def compute(bytes: Array[Byte]): SHA256Digest = {
    // Actual hashing is done by the platform implementation
    new SHA256Digest(SHA256DigestImpl.compute(bytes).toVector)
  }

  def fromVectorUnsafe(value: Vector[Byte]): SHA256Digest = {
    if (value.length == SHA256Digest.BYTE_LENGTH)
      new SHA256Digest(value)
    else
      throw new IllegalArgumentException(
        s"Vector length doesn't correspond to expected length  - ${SHA256Digest.BYTE_LENGTH}"
      )
  }

  def fromHexUnsafe(s: String): SHA256Digest = {
    if (HEX_STRING_RE.pattern.matcher(s).matches())
      SHA256Digest.fromVectorUnsafe(s.grouped(2).map(Integer.parseInt(_, 16).toByte).toVector)
    else throw new IllegalArgumentException(s"""The input string "$s" doesn't match regexp - "$HEX_STRING_RE"""")
  }
}
