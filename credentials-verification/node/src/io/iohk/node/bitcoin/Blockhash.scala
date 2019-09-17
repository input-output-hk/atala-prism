package io.iohk.node.bitcoin

import javax.xml.bind.DatatypeConverter

class Blockhash private (val string: String) extends AnyVal {

  def toBytesBE: List[Byte] = {
    string
      .grouped(2)
      .toList
      .map { hex =>
        Integer.parseInt(hex, 16).asInstanceOf[Byte]
      }
  }

  override def toString: String = string
}

object Blockhash {

  val Length = 64

  private val pattern = "^[a-f0-9]{64}$".r.pattern

  def from(string: String): Option[Blockhash] = {
    val lowercaseString = string.toLowerCase

    if (pattern.matcher(lowercaseString).matches()) {
      Some(new Blockhash(lowercaseString))
    } else {
      None
    }
  }

  def fromBytesBE(bytes: Array[Byte]): Option[Blockhash] = {
    val string = DatatypeConverter.printHexBinary(bytes)
    from(string)
  }
}
