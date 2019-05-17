package atala.view

import atala.obft.OuroborosBFT
import io.iohk.decco.Codec
import atala.obft.common.StateGate

import monix.reactive._
import atala.clock._
import atala.obft.common.StateSnapshot
import scala.concurrent.Future
import scala.concurrent.duration._
import atala.logging._

class InMemoryStateView[S, Tx, Q: Loggable, QR: Loggable](
    override protected val obftStateGate: StateGate[S],
    override protected val clockSignalsStream: Observable[StateViewActorMessage.Tick[S]],
    override protected val stateUpdatedEventStream: Observable[StateViewActorMessage.StateUpdatedEvent[S]]
)(initialState: S, override protected val slotDuration: FiniteDuration)(
    processQuery: (S, Q) => QR,
    transactionExecutor: (S, Tx) => Option[S]
) extends StateView[S, Tx, Q, QR] {

  // Private helpers
  // ---------------

  override protected final def stateSnapshot: StateSnapshot[S] = StateSnapshot(currentState, currentTimeSlot)

  private var currentState: S = initialState
  private var currentTimeSlot: TimeSlot = TimeSlot.zero

  override final protected def lastStateUpdateTimeSlot: TimeSlot = currentTimeSlot
  protected def updateState(newState: StateSnapshot[S]): Unit = {
    currentState = newState.computedState
    currentTimeSlot = newState.snapshotTimestamp
  }

  // Public interface
  // ----------------

  protected def runQuery(q: Q): Future[QR] =
    Future.successful(processQuery(currentState, q))

}

object InMemoryStateView {

  /**

      Generates an instance of StateView with all its external dependencies properly injected. To
      generate an instance of StateView and specify the external dependencies by hand (for example for
      testing purposes) use the constructor.

    */
  def apply[S, Tx: Codec, Q: Loggable, QR: Loggable](obft: OuroborosBFT[Tx])(
      initialState: S,
      stateRefreshInterval: FiniteDuration,
      slotDuration: FiniteDuration
  )(processQuery: (S, Q) => QR, transactionExecutor: (S, Tx) => Option[S]): InMemoryStateView[S, Tx, Q, QR] = {
    val obftStateGate: StateGate[S] = obft.gate[S](transactionExecutor)
    val clockSignalsStream: Observable[StateViewActorMessage.Tick[S]] = {
      Observable
        .intervalWithFixedDelay(delay = stateRefreshInterval, initialDelay = FiniteDuration(0, MILLISECONDS))
        .map { _ =>
          StateViewActorMessage.Tick()
        }
    }
    val stateUpdatedEventStream: Observable[StateViewActorMessage.StateUpdatedEvent[S]] =
      obftStateGate.stateUpdatedEventStream.map(StateViewActorMessage.StateUpdatedEvent.apply)

    new InMemoryStateView[S, Tx, Q, QR](obftStateGate, clockSignalsStream, stateUpdatedEventStream)(
      initialState,
      slotDuration
    )(processQuery, transactionExecutor)
  }

}
