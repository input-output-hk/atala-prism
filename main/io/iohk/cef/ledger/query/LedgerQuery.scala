package io.iohk.cef.ledger.query

import io.iohk.cef.query.Query

trait LedgerQuery[S] extends Query[LedgerQueryEngine[S]]
