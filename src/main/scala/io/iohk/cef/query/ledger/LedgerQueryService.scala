package io.iohk.cef.query.ledger

import io.iohk.cef.query.QueryService

case class LedgerQueryService[S, Q <: LedgerQuery[S]](override protected val engine: LedgerQueryEngine[S])
    extends QueryService[LedgerQueryEngine[S], Q]
