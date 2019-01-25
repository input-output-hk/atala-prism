package io.iohk.cef.ledger

import scala.concurrent.duration.{Duration, FiniteDuration}

case class LedgerConfig(
    id: LedgerId,
    maxBlockSize: Int,
    defaultTransactionExpiration: Duration,
    blockCreatorInitialDelay: FiniteDuration,
    blockCreatorInterval: FiniteDuration
)
