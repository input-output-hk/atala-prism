package io.iohk.node.cardano.models

import com.typesafe.config.ConfigMemorySize
import io.iohk.node.models.{HashValue, HashValueConfig, HashValueFrom}

import scala.collection.compat.immutable.ArraySeq

class WalletId private (val value: ArraySeq[Byte]) extends AnyVal with HashValue {}

object WalletId extends HashValueFrom[WalletId] {
  override val config: HashValueConfig = HashValueConfig(ConfigMemorySize.ofBytes(20))

  override def constructor(value: ArraySeq[Byte]): WalletId = new WalletId(value)
}
