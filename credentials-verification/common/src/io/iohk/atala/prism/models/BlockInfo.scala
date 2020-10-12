package io.iohk.atala.prism.models

import java.time.Instant

case class BlockInfo(
    number: Int, // Number of the block in the ledger
    timestamp: Instant, // When the block was created
    index: Int // Index of the transaction within the block
)
