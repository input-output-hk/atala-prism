package io.iohk.atala.prism.node.cardano.models

import com.typesafe.config.ConfigMemorySize
import io.iohk.atala.prism.models.{HashValue, HashValueConfig, HashValueFrom}

import scala.collection.compat.immutable.ArraySeq

class BlockHash private (val value: ArraySeq[Byte]) extends AnyVal with HashValue {}

object BlockHash extends HashValueFrom[BlockHash] {
  override val config: HashValueConfig = HashValueConfig(
    ConfigMemorySize.ofBytes(32)
  )

  override protected def constructor(value: ArraySeq[Byte]): BlockHash =
    new BlockHash(value)
}
