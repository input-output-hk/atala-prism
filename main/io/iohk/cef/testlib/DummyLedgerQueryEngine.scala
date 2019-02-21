package io.iohk.cef.test

import io.iohk.cef.ledger.query.LedgerQueryEngine
import io.iohk.cef.ledger.storage.LedgerStateStorage

class DummyLedgerQueryEngine(ledgerStateStorage: LedgerStateStorage[String])
    extends LedgerQueryEngine[String](ledgerStateStorage)
