package io.iohk.atala.mirror.models

import java.time.Instant

final case class CardanoWalletAddress(
    address: CardanoAddress,
    walletId: CardanoWallet.Id,
    sequenceNo: Int,
    usedAt: Option[CardanoWalletAddress.UsedAt]
)

object CardanoWalletAddress {
  case class UsedAt(date: Instant) extends AnyVal
}

final case class CardanoWalletAddressWithWalletName(
    cardanoWalletAddress: CardanoWalletAddress,
    walletName: Option[String]
)
