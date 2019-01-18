package io.iohk.cef.config

import io.iohk.cef.consensus.ConsensusConfig
import io.iohk.cef.ledger.LedgerConfig
import io.iohk.cef.network.NetworkConfig

case class CefConfig(networkConfig: NetworkConfig, consensusConfig: ConsensusConfig, ledgerConfig: LedgerConfig)
