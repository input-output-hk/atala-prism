package io.iohk.atala.prism.node.cardano.models

import com.typesafe.config.ConfigMemorySize
import io.iohk.atala.prism.models.{HashValue, HashValueConfig, HashValueFrom}
import tofu.logging.{DictLoggable, LogRenderer}

import scala.collection.compat.immutable.ArraySeq

class WalletId private (val value: ArraySeq[Byte]) extends AnyVal with HashValue {}

object WalletId extends HashValueFrom[WalletId] {

  implicit val walletIdLoggable: DictLoggable[WalletId] =
    new DictLoggable[WalletId] {
      override def fields[I, V, R, S](a: WalletId, i: I)(implicit
          r: LogRenderer[I, V, R, S]
      ): R =
        r.addString("WalletId", a.toString, i)

      override def logShow(a: WalletId): String =
        s"WalletId{${a.toString}"
    }

  override val config: HashValueConfig = HashValueConfig(
    ConfigMemorySize.ofBytes(20)
  )

  override protected def constructor(value: ArraySeq[Byte]): WalletId =
    new WalletId(value)
}
