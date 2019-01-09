package io.iohk.cef.query.ledger.identity

import io.iohk.cef.query.ledger.LedgerQueryService

object IdentityQueryService {
  def apply(engine: IdentityQueryEngine): IdentityQueryService =
    LedgerQueryService(engine)
}
