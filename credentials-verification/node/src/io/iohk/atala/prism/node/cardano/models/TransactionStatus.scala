package io.iohk.atala.prism.node.cardano.models

import enumeratum.EnumEntry.Snakecase
import enumeratum.{Enum, EnumEntry}

sealed trait TransactionStatus extends EnumEntry with Snakecase
object TransactionStatus extends Enum[TransactionStatus] {
  val values = findValues

  case object Pending extends TransactionStatus
  case object InLedger extends TransactionStatus
}
