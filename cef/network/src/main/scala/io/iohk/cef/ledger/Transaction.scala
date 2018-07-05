package io.iohk.cef.ledger

trait Transaction[State <: LedgerState] extends (State => Either[LedgerError, State])
