package io.iohk.atala.prism.node.cardano.models

import com.typesafe.config.ConfigMemorySize
import io.iohk.atala.prism.models.{HashValue, HashValueConfig, HashValueFrom}
import tofu.logging.{DictLoggable, LogRenderer}

import scala.collection.compat.immutable.ArraySeq

class BlockHash private (val value: ArraySeq[Byte]) extends AnyVal with HashValue {}

object BlockHash extends HashValueFrom[BlockHash] {

  implicit val blockHashLoggable: DictLoggable[BlockHash] =
    new DictLoggable[BlockHash] {
      override def fields[I, V, R, S](a: BlockHash, i: I)(implicit
          r: LogRenderer[I, V, R, S]
      ): R =
        r.addString("BlockHash", a.toString, i)

      override def logShow(a: BlockHash): String =
        s"BlockHash{${a.toString}"
    }

  override val config: HashValueConfig = HashValueConfig(
    ConfigMemorySize.ofBytes(32)
  )

  override protected def constructor(value: ArraySeq[Byte]): BlockHash =
    new BlockHash(value)
}
