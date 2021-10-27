package io.iohk.atala.prism.node.models

import io.iohk.atala.prism.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.protos.node_internal
import io.iohk.atala.prism.utils.BytesOps
import tofu.logging.{DictLoggable, LogRenderer}

case class AtalaObjectId(value: Vector[Byte]) {
  def hexValue: String = BytesOps.bytesToHex(value)

  override def toString: String = s"AtalaObjectId($hexValue)"
}

object AtalaObjectId {

  implicit val atalaObjectIdLoggable: DictLoggable[AtalaObjectId] =
    new DictLoggable[AtalaObjectId] {
      override def fields[I, V, R, S](a: AtalaObjectId, i: I)(implicit
          r: LogRenderer[I, V, R, S]
      ): R =
        r.addString("AtalaObjectId", a.hexValue, i)

      override def logShow(a: AtalaObjectId): String =
        s"AtalaObjectId{${a.hexValue}"
    }

  def apply(value: Vector[Byte]): AtalaObjectId = {
    // temporary replace for require(value.length == SHA256Digest.getBYTE_LENGTH)
    // rewrite to safe version pls
    // will throw an error if something is wrong with the value
    val digestUnsafe = Sha256Digest.fromBytes(value.toArray).getValue
    new AtalaObjectId(digestUnsafe.toVector)
  }

  def of(atalaObject: node_internal.AtalaObject): AtalaObjectId = {
    of(atalaObject.toByteArray)
  }

  def of(bytes: Array[Byte]): AtalaObjectId = {
    val hash = Sha256.compute(bytes)
    AtalaObjectId(hash.getValue.toVector)
  }
}
