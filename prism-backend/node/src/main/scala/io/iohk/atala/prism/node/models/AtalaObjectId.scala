package io.iohk.atala.prism.node.models

import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.protos.node_internal
import io.iohk.atala.prism.util.BytesOps

case class AtalaObjectId(value: Vector[Byte]) {
  require(value.length == SHA256Digest.BYTE_LENGTH)

  def hexValue: String = BytesOps.bytesToHex(value)

  override def toString: String = s"AtalaObjectId($hexValue)"
}

object AtalaObjectId {
  def of(atalaObject: node_internal.AtalaObject): AtalaObjectId = {
    of(atalaObject.toByteArray)
  }

  def of(bytes: Array[Byte]): AtalaObjectId = {
    val hash = SHA256Digest.compute(bytes)
    AtalaObjectId(hash.value)
  }
}
