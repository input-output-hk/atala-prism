package io.iohk.atala.prism.models

import enumeratum.EnumEntry.Snakecase
import enumeratum.{Enum, EnumEntry}

sealed trait TransactionStatus extends EnumEntry with Snakecase
object TransactionStatus extends Enum[TransactionStatus] {
  val values = findValues

  case object Pending extends TransactionStatus
  case object Submitted extends TransactionStatus
  case object Expired extends TransactionStatus
  case object InLedger extends TransactionStatus
}
