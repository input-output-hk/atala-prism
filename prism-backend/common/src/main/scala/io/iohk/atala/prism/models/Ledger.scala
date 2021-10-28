package io.iohk.atala.prism.models

import derevo.derive
import enumeratum.{Enum, EnumEntry}
import tofu.logging.derivation.loggable

@derive(loggable)
sealed trait Ledger extends EnumEntry
object Ledger extends Enum[Ledger] {
  val values = findValues

  case object InMemory extends Ledger
  case object CardanoTestnet extends Ledger
  case object CardanoMainnet extends Ledger
}
