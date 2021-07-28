package io.iohk.atala.cvp.webextension.util

object ByteOps {
  // NOTE: Seq[Byte] is not used on purpose, scalapb ByteString is a Seq we can deal with it like that,
  // instead, we need to invoke byteString.toByteArray
  def convertBytesToHex(bytes: Array[Byte]): String = {
    val sb = new StringBuilder
    for (b <- bytes) {
      sb.append(String.format("%02x", b & 0xff))
    }

    sb.toString()
  }
}
