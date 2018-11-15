package io.iohk.cef.ledger
import java.time.Instant

case class BlockHeader(created: Instant = Instant.ofEpochMilli(0))
