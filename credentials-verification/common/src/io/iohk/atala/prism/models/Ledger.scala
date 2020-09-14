package io.iohk.atala.prism.models

import enumeratum.{Enum, EnumEntry}

sealed trait Ledger extends EnumEntry
object Ledger extends Enum[Ledger] {
  val values = findValues

  case object InMemory extends Ledger
  case object BitcoinTestnet extends Ledger
  case object BitcoinMainnet extends Ledger
  case object CardanoTestnet extends Ledger
  case object CardanoMainnet extends Ledger
}
