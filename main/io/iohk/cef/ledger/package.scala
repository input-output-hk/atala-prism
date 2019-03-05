package io.iohk.cef

import io.iohk.cef.ledger.query.{LedgerQuery, LedgerQueryService}
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive.{Observable, Observer}

package object ledger {

  type LedgerId = String

  /**
    * A stream to get the transactions that are proposed but still not applied.
    */
  type ProposedTransactionsObservable[Tx] = Observable[Tx]

  /**
    * A stream to put the transactions to propose.
    */
  type ProposedTransactionsObserver[Tx] = Observer[Tx]

  type ProposedTransactionsSubject[Tx] = ConcurrentSubject[Tx, Tx]

  /**
    * A stream to get the blocks that are proposed but still not applied.
    */
  type ProposedBlocksObservable[State, Tx <: Transaction[State]] = Observable[Block[State, Tx]]

  /**
    * A stream to put the blocks to propose.
    */
  type ProposedBlocksObserver[State, Tx <: Transaction[State]] = Observer[Block[State, Tx]]

  type ProposedBlocksSubject[State, Tx <: Transaction[State]] = ConcurrentSubject[Block[State, Tx], Block[State, Tx]]

  /**
    * A stream to get the blocks that get applied.
    */
  type AppliedBlocksObservable[State, Tx <: Transaction[State]] = Observable[Block[State, Tx]]

  /**
    * A stream to put the blocks that get applied.
    */
  type AppliedBlocksObserver[State, Tx <: Transaction[State]] = Observer[Block[State, Tx]]

  type AppliedBlocksSubject[State, Tx <: Transaction[State]] = ConcurrentSubject[Block[State, Tx], Block[State, Tx]]

  type LedgerServicesMap[State, Tx <: Transaction[State], Q <: LedgerQuery[State]] =
    Map[LedgerId, LedgerServices[State, Tx, Q]]

  /**
    * Services that belong to a single ledger.
    */
  class LedgerServices[State, Tx <: Transaction[State], Q <: LedgerQuery[State]](
      proposedTransactionsSubject: ProposedTransactionsSubject[Tx],
      proposedBlocksSubject: ProposedBlocksSubject[State, Tx],
      appliedBlocksSubject: AppliedBlocksSubject[State, Tx],
      val ledgerQueryService: LedgerQueryService[State, Q]
  ) {

    def proposedTransactionsObservable: ProposedTransactionsObservable[Tx] = proposedTransactionsSubject

    def proposedTransactionsObserver: ProposedTransactionsObserver[Tx] = proposedTransactionsSubject

    def proposedBlocksObservable: ProposedBlocksObservable[State, Tx] = proposedBlocksSubject

    def proposedBlocksObserver: ProposedBlocksObserver[State, Tx] = proposedBlocksSubject

    def appliedBlocksObservable: AppliedBlocksObservable[State, Tx] = appliedBlocksSubject

    def appliedBlocksObserver: AppliedBlocksObserver[State, Tx] = appliedBlocksSubject
  }
}
