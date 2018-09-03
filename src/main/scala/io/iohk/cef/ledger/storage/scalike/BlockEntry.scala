package io.iohk.cef.ledger.storage.scalike

import java.time.Instant

import akka.util.ByteString
import io.iohk.cef.LedgerId

case class BlockEntry(
    ledgerId: LedgerId,
    blockNumber: Long,
    previousBlockId: Option[Long],
    createdOn: Instant,
    data: ByteString)
