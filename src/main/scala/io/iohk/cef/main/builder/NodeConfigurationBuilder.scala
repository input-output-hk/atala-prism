package io.iohk.cef.main.builder
import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.LedgerConfig
trait NodeConfigurationBuilder {
  val ledgersConfig: Map[LedgerId, LedgerConfig]
}
