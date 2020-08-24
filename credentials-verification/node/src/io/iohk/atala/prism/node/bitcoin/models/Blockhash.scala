package io.iohk.atala.prism.node.bitcoin.models

import com.typesafe.config.ConfigMemorySize
import io.iohk.atala.prism.node.models.{HashValue, HashValueConfig, HashValueFrom}

import scala.collection.compat.immutable.ArraySeq

class Blockhash private (val value: ArraySeq[Byte]) extends AnyVal with HashValue {}

object Blockhash extends HashValueFrom[Blockhash] {
  override val config: HashValueConfig = HashValueConfig(ConfigMemorySize.ofBytes(32))

  override def constructor(value: ArraySeq[Byte]): Blockhash = new Blockhash(value)
}
