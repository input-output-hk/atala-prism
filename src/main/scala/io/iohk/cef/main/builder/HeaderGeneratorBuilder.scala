package io.iohk.cef.main.builder

import io.iohk.cef.crypto._
import io.iohk.cef.ledger.{BlockHeader, Transaction}
import io.iohk.cef.ledger.identity.IdentityBlockHeader

trait LedgerHeaderGenerator[S, H <: BlockHeader] {
  val headerGenerator: Seq[Transaction[S]] => H
}

class IdentityLedgerHeaderGenerator(ledgerConfigBuilder: LedgerConfigBuilder)
    extends LedgerHeaderGenerator[Set[SigningPublicKey], IdentityBlockHeader] {
  import ledgerConfigBuilder._
  override val headerGenerator: Seq[Transaction[Set[SigningPublicKey]]] => IdentityBlockHeader = _ => {
    IdentityBlockHeader(clock.instant())
  }
}
