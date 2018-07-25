package io.iohk.cef.ledger.identity

import java.time.Instant

import akka.util.ByteString
import io.iohk.cef.ledger.BlockHeader

case class IdentityBlockHeader(hash: ByteString, created: Instant, height: Long) extends BlockHeader
