package io.iohk.cef.db

import java.time.Instant

import io.iohk.cef.network.Node

case class BlacklistNode(node: Node, blacklistSince: Instant, blacklistUntil: Instant)
