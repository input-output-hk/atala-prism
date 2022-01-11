package io.iohk.atala.prism.models

import derevo.derive
import tofu.logging.derivation.loggable

import java.time.Instant

@derive(loggable)
case class BlockInfo(
    number: Int, // Number of the block in the ledger
    timestamp: Instant, // When the block was created
    index: Int // Index of the transaction within the block
)
