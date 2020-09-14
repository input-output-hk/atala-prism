package io.iohk.atala.prism

import doobie.util.Meta

package object models {
  implicit val transactionIdMeta: Meta[TransactionId] =
    Meta[Array[Byte]].timap(
      TransactionId.from(_).getOrElse(throw new RuntimeException("Corrupted transaction ID"))
    )(_.value.toArray)
}
