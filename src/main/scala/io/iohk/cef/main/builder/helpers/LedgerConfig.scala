package io.iohk.cef.main.builder.helpers
import scala.concurrent.duration.{Duration, FiniteDuration}

class LedgerConfig(
    val maxBlockSizeInBytes: Int,
    val defaultTransactionExpiration: Duration,
    val blockCreatorInitialDelay: FiniteDuration,
    val blockCreatorInterval: FiniteDuration
)
