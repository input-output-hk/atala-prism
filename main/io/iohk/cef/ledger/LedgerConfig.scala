package io.iohk.cef.ledger

import scala.concurrent.duration.Duration

case class LedgerConfig(
    id: LedgerId,
    maxBlockSize: Int,
    defaultTransactionExpiration: Duration,
    blockCreatorInitialDelay: Duration,
    blockCreatorInterval: Duration
)
