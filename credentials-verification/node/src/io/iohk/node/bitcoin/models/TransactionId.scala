package io.iohk.node.bitcoin.models

import io.iohk.node.models.SHA256Value

class TransactionId private (val string: String) extends AnyVal with SHA256Value {

  override def toString: String = string
}

object TransactionId {

  def from(string: String): Option[TransactionId] = {
    SHA256Value.from(string).map(x => new TransactionId(x.string))
  }

  def fromBytesBE(bytes: Array[Byte]): Option[TransactionId] = {
    SHA256Value.fromBytesBE(bytes).map(x => new TransactionId(x.string))
  }

}
