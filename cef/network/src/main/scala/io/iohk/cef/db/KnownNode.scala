package io.iohk.cef.db

import java.time.Instant

import io.iohk.cef.network.Node

case class KnownNode(node: Node, discovered: Instant, lastSeen: Instant)

