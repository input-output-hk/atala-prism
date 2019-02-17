package io.iohk.cef.test

import io.iohk.cef.ledger.query.{LedgerQuery, LedgerQueryEngine}

class DummyLedgerQuery extends LedgerQuery[String] {

  override protected def perform(queryEngine: LedgerQueryEngine[String]): Response = ???
}
