package io.iohk.cef.main.builder

import io.iohk.cef.crypto._
import io.iohk.cef.ledger.identity.IdentityBlockHeader
import io.iohk.cef.ledger.{BlockHeader, Transaction}

trait HeaderGeneratorBuilder[S, H <: BlockHeader] {
  val headerGenerator: Seq[Transaction[S]] => H
}

trait IdentityLedgerHeaderGenerator extends HeaderGeneratorBuilder[Set[SigningPublicKey], IdentityBlockHeader] {
  self: LedgerConfigBuilder =>
  override val headerGenerator: Seq[Transaction[Set[SigningPublicKey]]] => IdentityBlockHeader = _ => {
    IdentityBlockHeader(clock.instant())
  }
}
