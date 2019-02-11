package io.iohk.cef.ledger.query.chimeric

import io.iohk.cef.ledger.query.LedgerQueryService

object ChimericQueryService {
  def apply(engine: ChimericQueryEngine): ChimericQueryService =
    LedgerQueryService(engine)
}
