package io.iohk.cef.network.discovery.db

import java.time.Instant

import io.iohk.cef.network.NodeInfo

case class KnownNode(node: NodeInfo, discovered: Instant, lastSeen: Instant)

