package io.iohk.cef.ledger

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config
import io.iohk.cef.LedgerId

import scala.concurrent.duration.{Duration, FiniteDuration}

class LedgerConfig(
    val id: LedgerId,
    val maxBlockSizeInBytes: Int,
    val defaultTransactionExpiration: Duration,
    val blockCreatorInitialDelay: FiniteDuration,
    val blockCreatorInterval: FiniteDuration
)

object LedgerConfig {
  def apply(config: Config): LedgerConfig = {
    val ledgerConfig = config.getConfig("ledger")
    new LedgerConfig(
      ledgerConfig.getString("id"),
      ledgerConfig.getInt("maxBlockSizeInBytes"),
      Duration(ledgerConfig.getString("defaultTransactionExpiration")),
      FiniteDuration(ledgerConfig.getDuration("blockCreatorInitialDelay").toNanos, TimeUnit.NANOSECONDS),
      FiniteDuration(ledgerConfig.getDuration("blockCreatorInterval").toNanos, TimeUnit.NANOSECONDS)
    )
  }
}
