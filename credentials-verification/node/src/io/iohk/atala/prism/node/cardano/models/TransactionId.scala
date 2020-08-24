package io.iohk.atala.prism.node.cardano.models

import com.typesafe.config.ConfigMemorySize
import io.iohk.atala.prism.node.models.{HashValue, HashValueConfig, HashValueFrom}

import scala.collection.compat.immutable.ArraySeq

class TransactionId private (val value: ArraySeq[Byte]) extends AnyVal with HashValue {}

object TransactionId extends HashValueFrom[TransactionId] {
  override val config: HashValueConfig = HashValueConfig(ConfigMemorySize.ofBytes(32))

  override def constructor(value: ArraySeq[Byte]): TransactionId = new TransactionId(value)
}
