package io.iohk.atala.prism.node.models

import io.iohk.atala.prism.kotlin.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.protos.node_internal
import io.iohk.atala.prism.utils.BytesOps

case class AtalaObjectId(value: Vector[Byte]) {
  def hexValue: String = BytesOps.bytesToHex(value)

  override def toString: String = s"AtalaObjectId($hexValue)"
}

object AtalaObjectId {

  def apply(value: Vector[Byte]): AtalaObjectId = {
    // temporary replace for require(value.length == SHA256Digest.getBYTE_LENGTH)
    // rewrite to safe version pls
    val canThrowException = Sha256Digest.fromBytes(value.toArray).getValue
    new AtalaObjectId(canThrowException.toVector)
  }

  def of(atalaObject: node_internal.AtalaObject): AtalaObjectId = {
    of(atalaObject.toByteArray)
  }

  def of(bytes: Array[Byte]): AtalaObjectId = {
    val hash = Sha256.compute(bytes)
    AtalaObjectId(hash.getValue.toVector)
  }
}
