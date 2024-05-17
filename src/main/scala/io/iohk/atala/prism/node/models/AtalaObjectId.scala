package io.iohk.atala.prism.node.models

import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.node.utils.BytesOps
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
    // This will throw an error if something is wrong with the value
    val digestUnsafe = Sha256Hash.fromBytes(value.toArray).bytes
    new AtalaObjectId(digestUnsafe)
  }

  def of(atalaObject: node_models.AtalaObject): AtalaObjectId = {
    of(atalaObject.toByteArray)
  }

  def of(bytes: Array[Byte]): AtalaObjectId = {
    val hash = Sha256Hash.compute(bytes)
    AtalaObjectId(hash.bytes)
  }
}
