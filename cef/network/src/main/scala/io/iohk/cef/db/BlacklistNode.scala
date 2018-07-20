package io.iohk.cef.db

import java.time.Instant

import io.iohk.cef.network.NodeInfo

case class BlacklistNode(nodeInfo: NodeInfo, blacklistSince: Instant, blacklistUntil: Instant)
