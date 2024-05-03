package io.iohk.atala.prism.node.models

import com.typesafe.config.ConfigMemorySize
import tofu.logging.{DictLoggable, LogRenderer}

import scala.collection.compat.immutable.ArraySeq

class TransactionId private (val value: ArraySeq[Byte]) extends AnyVal with HashValue {}

object TransactionId extends HashValueFrom[TransactionId] {

  implicit val transactionIdLoggable: DictLoggable[TransactionId] =
    new DictLoggable[TransactionId] {
      override def fields[I, V, R, S](a: TransactionId, i: I)(implicit
          r: LogRenderer[I, V, R, S]
      ): R =
        r.addString("TransactionId", a.toString, i)

      override def logShow(a: TransactionId): String =
        s"TransactionId{${a.toString}"
    }

  override val config: HashValueConfig = HashValueConfig(
    ConfigMemorySize.ofBytes(32)
  )

  override protected def constructor(value: ArraySeq[Byte]): TransactionId =
    new TransactionId(value)
}
