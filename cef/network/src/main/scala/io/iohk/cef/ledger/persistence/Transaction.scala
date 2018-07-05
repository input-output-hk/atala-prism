package io.iohk.cef.ledger.persistence

import scala.concurrent.Future

trait Transaction[State <: LedgerState] extends (State => Future[State])
