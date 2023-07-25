package io.iohk.atala.prism.models

import derevo.derive
import enumeratum.{Enum, EnumEntry}
import tofu.logging.derivation.loggable
import io.iohk.atala.prism.protos.common_models

@derive(loggable)
sealed trait Ledger extends EnumEntry {
  def toProto: common_models.Ledger
}
object Ledger extends Enum[Ledger] {
  val values = findValues

  case object InMemory extends Ledger {
    override def toProto: common_models.Ledger = common_models.Ledger.IN_MEMORY
  }

  case object CardanoTestnet extends Ledger {
    override def toProto: common_models.Ledger = common_models.Ledger.CARDANO_TESTNET
  }

  case object CardanoMainnet extends Ledger {
    override def toProto: common_models.Ledger = common_models.Ledger.CARDANO_MAINNET
  }
}
