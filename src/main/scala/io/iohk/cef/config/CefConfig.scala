package io.iohk.cef.config

import io.iohk.cef.ledger.LedgerConfig
import io.iohk.network.NetworkConfig

case class CefConfig(networkConfig: NetworkConfig, consensusConfig: ConsensusConfig, ledgerConfig: LedgerConfig)
