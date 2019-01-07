package io.iohk.query.ledger

import io.iohk.query.QueryService

case class LedgerQueryService[S, Q <: LedgerQuery[S]](override protected val engine: LedgerQueryEngine[S])
  extends QueryService[LedgerQueryEngine[S], Q]
