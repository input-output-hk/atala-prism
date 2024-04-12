package io.iohk.atala.prism.node.models

import derevo.derive
import enumeratum.EnumEntry.Snakecase
import enumeratum.{Enum, EnumEntry}
import tofu.logging.derivation.loggable

@derive(loggable)
sealed trait TransactionStatus extends EnumEntry with Snakecase
object TransactionStatus extends Enum[TransactionStatus] {
  val values = findValues

  case object Pending extends TransactionStatus
  case object Submitted extends TransactionStatus
  case object Expired extends TransactionStatus
  case object InLedger extends TransactionStatus
}
