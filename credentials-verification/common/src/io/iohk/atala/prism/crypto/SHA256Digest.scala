package io.iohk.atala.prism.crypto

import java.security.MessageDigest

case class SHA256Digest(value: Array[Byte]) {
  require(value.length == SHA256Digest.BYTE_LENGTH)

  def hexValue: String = value.map("%02x".format(_)).mkString("")

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
  val HEX_STRING_RE = "^(?:[0-9a-fA-F]{2})+$".r

  private def messageDigest = MessageDigest.getInstance("SHA-256")

  def compute(data: Array[Byte]): SHA256Digest = {
    SHA256Digest(messageDigest.digest(data))
  }

  def fromHex(s: String): SHA256Digest = {
    assert(HEX_STRING_RE.pattern.matcher(s).matches())
    SHA256Digest(s.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray)
  }
}
