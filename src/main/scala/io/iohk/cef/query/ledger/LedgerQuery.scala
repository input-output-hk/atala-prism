package io.iohk.query.ledger

import io.iohk.query.Query

trait LedgerQuery[S] extends Query[LedgerQueryEngine[S]]
