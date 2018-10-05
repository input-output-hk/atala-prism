package io.iohk.cef.ledger.identity

import java.time.Instant

import io.iohk.cef.ledger.BlockHeader

case class IdentityBlockHeader(created: Instant) extends BlockHeader
