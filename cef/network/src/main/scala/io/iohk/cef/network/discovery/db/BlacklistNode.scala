package io.iohk.cef.network.discovery.db

import java.time.Instant

import io.iohk.cef.network.NodeInfo

case class BlacklistNode(nodeInfo: NodeInfo, blacklistSince: Instant, blacklistUntil: Instant)
