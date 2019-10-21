package io.iohk.node.services

import io.iohk.node.atala_bitcoin._

trait BinaryOps {
  def toBytes(tx: AtalaBlock): Array[Byte]
  def toBytes(obj: AtalaObject): Array[Byte]
  def genHash(bytes: Array[Byte]): Array[Byte]

  final def hash(tx: AtalaBlock): Array[Byte] =
    genHash(toBytes(tx))

  final def hash(obj: AtalaObject): Array[Byte] =
    genHash(toBytes(obj))

  final def hashHex(tx: AtalaBlock): String =
    convertBytesToHex(hash(tx))

  final def hashHex(obj: AtalaObject): String =
    convertBytesToHex(hash(obj))

  private def convertBytesToHex(bytes: Seq[Byte]): String = {
    val sb = new StringBuilder
    for (b <- bytes) {
      sb.append(String.format("%02x", Byte.box(b)))
    }
    sb.toString
  }
}

object BinaryOps {
  def apply(): BinaryOps = DefaultBinaryOps
}

object DefaultBinaryOps extends BinaryOps {

  override def toBytes(tx: AtalaBlock): Array[Byte] =
    tx.toByteArray

  override def toBytes(obj: AtalaObject): Array[Byte] =
    obj.toByteArray

  override def genHash(bytes: Array[Byte]): Array[Byte] = {
    import java.security.MessageDigest
    val digest = MessageDigest.getInstance("SHA-256")
    digest.digest(bytes)
  }
}
