package io.iohk.cef.config
import io.iohk.cef.consensus.ConsensusConfig
import io.iohk.cef.ledger.LedgerConfig
import io.iohk.cef.network.PeerConfig
import io.iohk.cef.network.discovery.DiscoveryConfig

case class CefConfig(
    peerConfig: PeerConfig,
    discoveryConfig: DiscoveryConfig,
    consensusConfig: ConsensusConfig,
    ledgerConfig: LedgerConfig
)
