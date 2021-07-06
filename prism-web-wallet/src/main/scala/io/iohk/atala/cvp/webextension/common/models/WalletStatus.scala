package io.iohk.atala.cvp.webextension.common.models

sealed trait WalletStatus

object WalletStatus {

  final case object Missing extends WalletStatus
  final case object Unlocked extends WalletStatus
  final case object Locked extends WalletStatus
}
