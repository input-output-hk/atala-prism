package io.iohk.cef.query.chimeric

import io.iohk.cef.query.ledger.LedgerQueryService

object ChimericQueryService {
  def apply(engine: ChimericQueryEngine): ChimericQueryService =
    LedgerQueryService(engine)
}
