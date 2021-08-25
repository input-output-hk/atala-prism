package io.iohk.atala.prism.node.models

import enumeratum.{CirceEnum, Enum}
import enumeratum.EnumEntry.Snakecase
import io.iohk.atala.prism.node.cardano.models.Lovelace

final case class WalletDetails(balance: Balance, state: WalletState)

final case class Balance(available: Lovelace)

final case class WalletState(status: WalletStatus)

sealed trait WalletStatus extends Snakecase

object WalletStatus extends Enum[WalletStatus] with CirceEnum[WalletStatus] {
  lazy val values: IndexedSeq[WalletStatus] = findValues
  final case object Ready extends WalletStatus
  final case object Syncing extends WalletStatus
  final case object NotResponding extends WalletStatus
}
