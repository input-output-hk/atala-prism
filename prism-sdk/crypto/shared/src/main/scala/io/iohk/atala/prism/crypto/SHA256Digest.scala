package io.iohk.atala.prism.crypto

case class SHA256Digest(value: Vector[Byte]) {
  require(value.length == SHA256Digest.BYTE_LENGTH)

  def hexValue: String = ECUtils.bytesToHex(value.toArray)

  override def toString: String = s"SHA256Digest($hexValue)"
}

object SHA256Digest {
  val BYTE_LENGTH = 32
  val HEX_STRING_RE = "^(?:[0-9a-fA-F]{2})+$".r

  def compute(bytes: Array[Byte]): SHA256Digest = {
    // Actual hashing is done by the platform implementation
    new SHA256Digest(SHA256DigestImpl.compute(bytes).toVector)
  }

  def fromHex(s: String): SHA256Digest = {
    assert(HEX_STRING_RE.pattern.matcher(s).matches())
    SHA256Digest(s.grouped(2).map(Integer.parseInt(_, 16).toByte).toVector)
  }
}
