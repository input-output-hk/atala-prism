package io.iohk.atala.cvp.webextension.util

object ByteOps {
  def convertBytesToHex(bytes: Array[Byte]): String =
    convertBytesToHex(bytes.toSeq)

  def convertBytesToHex(bytes: Seq[Byte]): String = {
    val sb = new StringBuilder
    for (b <- bytes) {
      sb.append(String.format("%02x", Byte.box(b)))
    }
    sb.toString
  }
}
