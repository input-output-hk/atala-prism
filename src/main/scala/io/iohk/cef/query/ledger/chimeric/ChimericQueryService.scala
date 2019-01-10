package io.iohk.cef.query.ledger.chimeric

import io.iohk.cef.query.ledger.LedgerQueryService

object ChimericQueryService {
  def apply(engine: ChimericQueryEngine): ChimericQueryService =
    LedgerQueryService(engine)
}
