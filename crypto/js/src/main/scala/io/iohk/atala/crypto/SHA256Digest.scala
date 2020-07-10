package io.iohk.atala.crypto

import typings.hashJs.{mod => hash}

import scala.scalajs.js.typedarray.{Uint8Array, _}

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
  private val HEX_STRING_RE = "^(?:[0-9a-fA-F]{2})+$".r

  def compute(bytes: Array[Byte]): SHA256Digest = {
    val byteArray = bytes.toTypedArray
    val uint8Array = new Uint8Array(byteArray.buffer, byteArray.byteOffset, byteArray.length)
    val sha256 = hash.sha256().update(uint8Array)
    SHA256Digest(sha256.digest().toArray map (_.toByte))
  }
}
