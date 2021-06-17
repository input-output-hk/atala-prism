package io.iohk.atala.mirror.models

import java.time.Instant

case class CardanoAddressWithUsageInfo(cardanoAddress: CardanoAddress, usedAt: Instant)
