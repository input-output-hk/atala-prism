package io.iohk.atala.crypto

case class SHA256Digest(value: Array[Byte]) {
  require(value.length == SHA256Digest.BYTE_LENGTH)

  def hexValue: String = value.map(b => "%02x".format(b & 0xff)).mkString("")

  override def canEqual(that: Any): Boolean = that.isInstanceOf[SHA256Digest]

  override def equals(obj: Any): Boolean = {
    canEqual(obj) && (obj match {
      case SHA256Digest(otherValue) => value.sameElements(otherValue)
      case _ => false
    })
  }

  override def toString(): String = s"SHA256Digest($hexValue)"
}

object SHA256Digest {
  val BYTE_LENGTH = 32

  def compute(bytes: Array[Byte]): SHA256Digest = {
    // Actual hashing is done by the platform implementation
    new SHA256Digest(SHA256DigestImpl.compute(bytes))
  }
}
