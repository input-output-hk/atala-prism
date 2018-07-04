package io.iohk.cef.ledger.typeParams

import io.iohk.cef.ledger.LedgerError

trait Transaction[State <: LedgerState] extends (State => Either[LedgerError, State])
