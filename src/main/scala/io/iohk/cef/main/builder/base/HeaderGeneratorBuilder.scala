package io.iohk.cef.main.builder.base
import io.iohk.cef.ledger.identity.IdentityBlockHeader
import io.iohk.cef.ledger.{BlockHeader, Transaction}

trait HeaderGeneratorBuilder[S, H <: BlockHeader] {
  val headerGenerator: Seq[Transaction[S]] => H
}

trait IdentityLedgerHeaderGenerator extends HeaderGeneratorBuilder[String, IdentityBlockHeader] {
  self: LedgerConfigBuilder =>
  override val headerGenerator: Seq[Transaction[String]] => IdentityBlockHeader = _ => {
    IdentityBlockHeader(clock.instant())
  }
}
