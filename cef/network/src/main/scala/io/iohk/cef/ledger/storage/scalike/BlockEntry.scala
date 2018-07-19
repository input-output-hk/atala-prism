package io.iohk.cef.ledger.storage.scalike

import java.time.Instant

import akka.util.ByteString

case class BlockEntry(ledgerId: Int, blockNumber: Long, previousBlockId: Option[Long], createdOn: Instant, data: ByteString)
