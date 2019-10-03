package io.iohk.node.bitcoin.models

import io.iohk.node.models.SHA256Value

class Blockhash private (val string: String) extends AnyVal with SHA256Value {

  override def toString: String = string
}

object Blockhash {

  def from(string: String): Option[Blockhash] = {
    SHA256Value.from(string).map(x => new Blockhash(x.string))
  }

  def fromBytesBE(bytes: Array[Byte]): Option[Blockhash] = {
    SHA256Value.fromBytesBE(bytes).map(x => new Blockhash(x.string))
  }
}
