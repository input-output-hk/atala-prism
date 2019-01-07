package io.iohk.query.ledger.identity

import io.iohk.query.ledger.LedgerQueryService

object IdentityQueryService {
  def apply(engine: IdentityQueryEngine): IdentityQueryService =
    LedgerQueryService(engine)
}
