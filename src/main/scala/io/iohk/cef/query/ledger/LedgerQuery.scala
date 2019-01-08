package io.iohk.cef.query.ledger

import io.iohk.cef.query.Query

trait LedgerQuery[S] extends Query[LedgerQueryEngine[S]]
