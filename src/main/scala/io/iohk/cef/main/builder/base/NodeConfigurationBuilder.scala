package io.iohk.cef.main.builder.base
import io.iohk.cef.LedgerId
import io.iohk.cef.main.builder.helpers.LedgerConfig

trait NodeConfigurationBuilder {
  val ledgersConfig: Map[LedgerId, LedgerConfig]
}
