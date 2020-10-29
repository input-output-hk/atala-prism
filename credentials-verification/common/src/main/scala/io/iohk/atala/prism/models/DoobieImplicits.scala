package io.iohk.atala.prism.models

import doobie.Meta
import doobie.util.invariant.InvalidEnum

object DoobieImplicits {
  implicit val transactionIdMeta: Meta[TransactionId] =
    Meta[Array[Byte]].timap(b =>
      TransactionId.from(b).getOrElse(throw new IllegalArgumentException("Unexpected TransactionId"))
    )(_.value.toArray)

  implicit val ledgerMeta: Meta[Ledger] =
    Meta[String].timap(b => Ledger.withNameInsensitiveOption(b).getOrElse(throw InvalidEnum[Ledger](b)))(_.entryName)
}
