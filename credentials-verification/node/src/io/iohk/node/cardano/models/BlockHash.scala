package io.iohk.node.cardano.models

import io.iohk.node.models.SHA256Value

class BlockHash private (val string: String) extends AnyVal with SHA256Value {

  override def toString: String = string
}

object BlockHash {

  def from(string: String): Option[BlockHash] = {
    SHA256Value.from(string).map(x => new BlockHash(x.string))
  }

  def fromBytesBE(bytes: Array[Byte]): Option[BlockHash] = {
    SHA256Value.fromBytesBE(bytes).map(x => new BlockHash(x.string))
  }
}
