package atala.state

import monix.reactive._
import atala.helpers.monixhelpers._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.ConcurrentSubject

import atala.logging._

class StateGate[S, T](
    actionsStream: Observer[StateGate.Callback]
)(
    prepareComputation: StateGate.StateComputationStage[S, T],
    computeUpdatedState: StateGate.StateComputationStage[S, T],
    finalizeComputation: StateGate.StateComputationStage[S, T]
) extends AtalaLogging {

  // Public interface
  // ----------------

  def stateUpdatedEventStream: Observable[StateSnapshot[S, T]] = stateEventSubject

  def requestStateUpdate(now: T, previousSnapshot: StateSnapshot[S, T]): Unit = {

    logger.trace("State update requested to the StateGate")

    val executable: StateGate.Callback = () => {
      val c = prepareComputation(now, previousSnapshot)
      val newState =
        computeUpdatedState(now, StateSnapshot(c, previousSnapshot.snapshotTimestamp))

      logger.trace("New state computed in the StateGate")

      val finalState = finalizeComputation(now, StateSnapshot(newState, now))
      stateEventSubject.feedItem(StateSnapshot(finalState, now))
    }

    actionsStream.feedItem(executable)
  }

  // The stream
  // ----------

  private val stateEventSubject: Observer[StateSnapshot[S, T]] with Observable[StateSnapshot[S, T]] =
    ConcurrentSubject[StateSnapshot[S, T]](MulticastStrategy.replay)
}

object StateGate {
  type Callback = () => Unit
  type StateComputationStage[S, T] = ( /*now:*/ T, /*previousSnapshot:*/ StateSnapshot[S, T]) => S

  def apply[S, T](
      actionsStream: Observer[StateGate.Callback]
  )(
      prepareComputation: StateGate.StateComputationStage[S, T],
      computeUpdatedState: StateGate.StateComputationStage[S, T],
      finalizeComputation: StateGate.StateComputationStage[S, T]
  ): StateGate[S, T] = new StateGate[S, T](actionsStream)(prepareComputation, computeUpdatedState, finalizeComputation)
}
