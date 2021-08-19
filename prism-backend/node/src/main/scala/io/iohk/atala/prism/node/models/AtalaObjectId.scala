package io.iohk.atala.prism.node.models

import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.protos.node_internal
import io.iohk.atala.prism.utils.BytesOps

case class AtalaObjectId(value: Vector[Byte]) {
  require(value.length == SHA256Digest.getBYTE_LENGTH)

  def hexValue: String = BytesOps.bytesToHex(value)

  override def toString: String = s"AtalaObjectId($hexValue)"
}

object AtalaObjectId {
  def of(atalaObject: node_internal.AtalaObject): AtalaObjectId = {
    of(atalaObject.toByteArray)
  }

  def of(bytes: Array[Byte]): AtalaObjectId = {
    val hash = SHA256Digest.compute(bytes)
    AtalaObjectId(hash.getValue.toVector)
  }
}
