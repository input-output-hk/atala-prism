package io.iohk.atala.prism.node.models

import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.util.BytesOps

class AtalaOperationId private (val digest: SHA256Digest) {
  def value: Vector[Byte] = digest.value

  def hexValue: String = BytesOps.bytesToHex(value)

  def toProtoByteString: ByteString = ByteString.copyFrom(value.toArray)

  override def equals(obj: Any): Boolean =
    obj match {
      case that: AtalaOperationId => this.digest.equals(that.digest)
      case _ => false
    }

  override def hashCode(): Int = digest.hashCode()

  override def toString: String = s"OperationId($hexValue)"
}

object AtalaOperationId {
  def of(atalaOperation: node_models.SignedAtalaOperation): AtalaOperationId = {
    of(atalaOperation.toByteArray)
  }

  def of(bytes: Array[Byte]): AtalaOperationId = {
    val hash = SHA256Digest.compute(bytes)
    new AtalaOperationId(hash)
  }

  def fromVectorUnsafe(bytes: Vector[Byte]): AtalaOperationId = {
    val hash = SHA256Digest.fromVectorUnsafe(bytes)
    new AtalaOperationId(hash)
  }
}
