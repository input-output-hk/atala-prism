package io.iohk.cef.ledger.query.identity

import io.iohk.cef.ledger.query.LedgerQueryService

object IdentityQueryService {
  def apply(engine: IdentityQueryEngine): IdentityQueryService =
    LedgerQueryService(engine)
}
