package io.iohk.atala.prism.node.services

import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.node.services.models._
import io.iohk.prism.protos.node_internal._

trait BinaryOps {
  def toBytes(tx: AtalaBlock): Array[Byte]
  def toBytes(obj: AtalaObject): Array[Byte]
  def genHash(bytes: Array[Byte]): Array[Byte]

  def extractOpReturn(asm: String): Option[Array[Byte]]
  def trimZeros(in: Array[Byte]): Array[Byte]

  final def getId(obj: AtalaObject): AtalaObjectId =
    getBytesAndId(obj)._2

  final def getBytesAndId(obj: AtalaObject): (Array[Byte], AtalaObjectId) = {
    val bytes = toBytes(obj)
    val hash = genHash(bytes)
    (bytes, AtalaObjectId(SHA256Digest(hash)))
  }

  final def hash(tx: AtalaBlock): Array[Byte] =
    genHash(toBytes(tx))

  final def hash(obj: AtalaObject): Array[Byte] =
    genHash(toBytes(obj))

  final def hashHex(tx: AtalaBlock): String =
    convertBytesToHex(hash(tx))

  final def hashHex(obj: AtalaObject): String =
    convertBytesToHex(hash(obj))

  def convertBytesToHex(bytes: Seq[Byte]): String = {
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

  override def extractOpReturn(asm: String): Option[Array[Byte]] = {
    import javax.xml.bind.DatatypeConverter

    import scala.util.Try
    val HEAD = "OP_RETURN "
    if (asm.startsWith(HEAD)) {
      val hexData = asm.drop(HEAD.length)
      Try(DatatypeConverter.parseHexBinary(hexData)).toOption
    } else {
      None
    }
  }

  override def trimZeros(in: Array[Byte]): Array[Byte] = {
    var first = 0
    while (first < in.length && in(first) == 0) first += 1
    java.util.Arrays.copyOfRange(in, first, in.length)
  }

}
