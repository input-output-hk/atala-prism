package atala.obft

import monix.reactive._
import atala.clock._
import atala.helpers.monixhelpers._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.ConcurrentSubject
import atala.obft.blockchain._

class StateGate[S, Tx](
    actionsStream: Observer[ObftInternalActorMessage[Tx]] with Observable[ObftInternalActorMessage[Tx]],
    blockchain: Blockchain[Tx]
)(transactionExecutor: (S, Tx) => Option[S], useFinalizedTransactions: Boolean = true) {

  // Public interface
  // ----------------

  def stateUpdatedEventStream: Observable[StateSnapshot[S]] = stateEventSubject

  def requestStateUpdate(now: TimeSlot, previousSnapshot: StateSnapshot[S]): Unit = {
    val executable: () => Unit = () => {
      val newState =
        if (useFinalizedTransactions)
          runFinalizedTransactionsFromPreviousStateSnapshot(now, previousSnapshot, transactionExecutor)
        else
          unsafeRunTransactionsFromPreviousStateSnapshot(previousSnapshot, transactionExecutor)

      stateEventSubject.feedItem(StateSnapshot(newState, now))
    }

    actionsStream.feedItem(RequestStateUpdate(executable))
  }

  // The stream
  // ----------

  private val stateEventSubject: Observer[StateSnapshot[S]] with Observable[StateSnapshot[S]] =
    ConcurrentSubject[StateSnapshot[S]](MulticastStrategy.replay)

  // Helper methods
  // --------------

  private def unsafeRunAllTransactions[S](initialState: S, transactionExecutor: (S, Tx) => Option[S]): S =
    blockchain.unsafeRunAllTransactions(initialState, transactionExecutor)

  private def unsafeRunTransactionsFromPreviousStateSnapshot[S](
      snapshot: StateSnapshot[S],
      transactionExecutor: (S, Tx) => Option[S]
  ): S =
    blockchain.unsafeRunTransactionsFromPreviousStateSnapshot(snapshot, transactionExecutor)

  private def runAllFinalizedTransactions[S](
      now: TimeSlot,
      initialState: S,
      transactionExecutor: (S, Tx) => Option[S]
  ): S =
    blockchain.runAllFinalizedTransactions(now, initialState, transactionExecutor)

  private def runFinalizedTransactionsFromPreviousStateSnapshot[S](
      now: TimeSlot,
      snapshot: StateSnapshot[S],
      transactionExecutor: (S, Tx) => Option[S]
  ): S =
    blockchain.runFinalizedTransactionsFromPreviousStateSnapshot(now, snapshot, transactionExecutor)
}
