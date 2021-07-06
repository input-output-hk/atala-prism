package io.iohk.atala.mirror.models

import java.time.Instant

case class CardanoAddressBlockInfo(
    cardanoAddress: CardanoAddress,
    blockIssueTime: Instant,
    cardanoBlockId: CardanoBlockId
)
