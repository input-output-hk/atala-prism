package io.iohk.atala.prism.node.models

import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.protos.node_models
import tofu.logging.{DictLoggable, LogRenderer}
import io.iohk.atala.prism.node.utils.BytesOps

import java.util.UUID

class AtalaOperationId private (val digest: Sha256Digest) {
  def value: Vector[Byte] = digest.getValue.toVector

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

  implicit val atalaOperationLoggable: DictLoggable[AtalaOperationId] =
    new DictLoggable[AtalaOperationId] {
      override def fields[I, V, R, S](a: AtalaOperationId, i: I)(implicit
          r: LogRenderer[I, V, R, S]
      ): R =
        r.addString("AtalaOperationId", a.hexValue, i)

      override def logShow(a: AtalaOperationId): String =
        s"AtalaOperationId{${a.hexValue}"
    }

  def of(atalaOperation: node_models.SignedAtalaOperation): AtalaOperationId = {
    val hash = Sha256.compute(atalaOperation.toByteArray)
    new AtalaOperationId(hash)
  }

  def random(): AtalaOperationId = {
    val hash = Sha256.compute(UUID.randomUUID().toString.getBytes())
    new AtalaOperationId(hash)
  }

  def fromVectorUnsafe(bytes: Vector[Byte]): AtalaOperationId = {
    val hash = Sha256Digest.fromBytes(bytes.toArray)
    new AtalaOperationId(hash)
  }

  def fromHexUnsafe(hex: String): AtalaOperationId = {
    val hash = Sha256Digest.fromHex(hex)
    new AtalaOperationId(hash)
  }
}
