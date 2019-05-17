package atala.view

import doobie._
import atala.obft.OuroborosBFT
import atala.obft.common.StateGate

import io.iohk.decco.Codec
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.ExecutionContext
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive._
import atala.clock._
import atala.obft.common.StateSnapshot
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration._
import atala.helpers.monixhelpers._
import scala.util.control.NonFatal
import atala.logging._

abstract class StateView[S, Tx, Q: Loggable, QR: Loggable] extends AtalaLogging {

  protected val obftStateGate: StateGate[S]
  protected val clockSignalsStream: Observable[StateViewActorMessage.Tick[S]]
  protected val stateUpdatedEventStream: Observable[StateViewActorMessage.StateUpdatedEvent[S]]
  protected val slotDuration: FiniteDuration

  // Public interface
  // ----------------

  protected def runQuery(q: Q): Future[QR]

  final def ask(q: Q): Future[QR] = {
    logger.trace("Starting view query", "query" -> q)
    val p: Promise[QR] = Promise[QR]()
    val action: () => Unit = () => {
      try {
        val qrf = runQuery(q)
        qrf.map { qr =>
          logger.debug("StateView queried", "query" -> q, "response" -> qr)
          qr
        }
        p.completeWith(qrf)
      } catch {
        case NonFatal(e) =>
          p.failure(e)
      }
      ()
    }

    actionsStream.feedItem(StateViewActorMessage.PerformQuery(action))
    p.future
  }

  final def run(): Unit =
    viewActorStream.subscribe()

  // Message processing method
  // -------------------------

  protected def lastStateUpdateTimeSlot: TimeSlot

  // This method is executed only by `processActorMessage`,
  // a method that, in turn, is only executed concurrently
  // by the `viewActorStream`
  protected def updateState(newState: StateSnapshot[S]): Unit
  protected def stateSnapshot: StateSnapshot[S]

  private def processActorMessage(message: StateViewActorMessage[S]): Unit =
    message match {

      case StateViewActorMessage.Tick() =>
        obftStateGate.requestStateUpdate(Clock.currentSlot(slotDuration), stateSnapshot)

      case StateViewActorMessage.StateUpdatedEvent(newState) =>
        if (newState.snapshotTimestamp > lastStateUpdateTimeSlot) {
          logger.trace("updating StateView internal state")
          updateState(newState)
          logger.debug("StateView internal state updated")
        }

      case StateViewActorMessage.PerformQuery(action) =>
        action()

    }

  // The Streams
  // -----------

  private val actionsStream
      : Observer[StateViewActorMessage.PerformQuery[S]] with Observable[StateViewActorMessage.PerformQuery[S]] = {
    ConcurrentSubject[StateViewActorMessage.PerformQuery[S]](MulticastStrategy.replay)
  }

  final private[view] val viewActorStream = {
    Observable
      .apply(
        stateUpdatedEventStream,
        clockSignalsStream,
        actionsStream
      )
      .merge
      .oneach(processActorMessage)
  }

}

object StateView {

  def doobieSqlStateView[Tx, Q: Loggable, QR: Loggable](
      obft: OuroborosBFT[Tx],
      xa: Transactor[Future]
  )(stateRefreshInterval: FiniteDuration, slotDuration: FiniteDuration)(
      transactionExecutor: Tx => ConnectionIO[Int],
      queryExecutor: Q => ConnectionIO[QR]
  )(implicit ec: ExecutionContext): DoobieSqlStateView[Tx, Q, QR] =
    DoobieSqlStateView(obft, xa)(stateRefreshInterval, slotDuration)(transactionExecutor, queryExecutor)

  /**

      Generates an instance of StateView with all its external dependencies properly injected. To
      generate an instance of StateView and specify the external dependencies by hand (for example for
      testing purposes) use the constructor.

    */
  def inMemory[S, Tx: Codec, Q: Loggable, QR: Loggable](obft: OuroborosBFT[Tx])(
      initialState: S,
      stateRefreshInterval: FiniteDuration,
      slotDuration: FiniteDuration
  )(processQuery: (S, Q) => QR, transactionExecutor: (S, Tx) => Option[S]): InMemoryStateView[S, Tx, Q, QR] =
    InMemoryStateView(obft)(initialState, stateRefreshInterval, slotDuration)(processQuery, transactionExecutor)

}
