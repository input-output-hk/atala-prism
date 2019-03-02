package io.iohk.cef

import io.iohk.cef.consensus.Consensus
import io.iohk.cef.ledger.query.{LedgerQuery, LedgerQueryService}
import io.iohk.cef.ledger.{LedgerId, Transaction}
import monix.reactive.subjects.ConcurrentSubject

package object transactionservice {

  type TransactionChannel[Tx] = ConcurrentSubject[Tx, Tx]

  type LedgerServicesMap[State, Tx <: Transaction[State], Q <: LedgerQuery[State]] =
    Map[LedgerId, LedgerServices[State, Tx, Q]]

  /**
    * Services that belong to a single ledger.
    */
  case class LedgerServices[State, Tx <: Transaction[State], Q <: LedgerQuery[State]](
      transactionChannel: TransactionChannel[Tx],
      consensus: Consensus[State, Tx],
      ledgerQueryService: LedgerQueryService[State, Q]
  )

}
