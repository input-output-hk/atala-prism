package atala.ledger

// format: off

import atala.obft.OuroborosBFT
import atala.obft.StateGate

import io.iohk.decco.Codec
import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive._
import atala.clock._
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration._
import atala.helpers.monixhelpers._
import scala.util.control.NonFatal
import atala.logging._

class Ledger[S, Tx, Q: Loggable, QR: Loggable](
    obftStateGate: StateGate[S, Tx],
    storage: StateStorage[S],
    clockSignalsStream: Observable[LedgerActorMessage.Tick[S]],
    stateUpdatedEventStream: Observable[LedgerActorMessage.StateUpdatedEvent[S]])(

    slotDuration: FiniteDuration)(

    processQuery: (S, Q) => QR,
    transactionExecutor: (S, Tx) => Option[S]) extends AtalaLogging {



  // Public interface
  // ----------------

  def ask(q: Q): Future[QR] = {
    logger.debug("Starting ledger query", "query" -> q)
    val p: Promise[QR] = Promise[QR]()
    val action: () => Unit = () => {
      try {
        val qr = processQuery(state, q)
        logger.info("Ledger queried", "query" -> q, "response" -> qr)
        p.success(qr)
      }
      catch {
        case NonFatal(e) =>
          p.failure(e)
      }
      ()
    }

    actionsStream.feedItem(LedgerActorMessage.PerformQuery(action))
    p.future
  }

  def run(): Unit =
    ledgerActorStream.subscribe()



  // Message processing method
  // -------------------------

  private def processActorMessage(message: LedgerActorMessage[S]): Unit =
    message match {

      case LedgerActorMessage.Tick() =>
        obftStateGate.requestStateUpdate(Clock.currentSlot(slotDuration), stateSnapshot)

      case LedgerActorMessage.StateUpdatedEvent(newState) =>
        if (newState.snapshotTimestamp > stateSnapshot.snapshotTimestamp) {
          logger.trace("Ledger state updated")
          storage.put(newState)
        }

      case LedgerActorMessage.PerformQuery(action) =>
        action()

    }



  // Private helpers
  // ---------------

  private def stateSnapshot: StateSnapshot[S] = storage.currentState
  private def state: S = stateSnapshot.computedState



  // The Streams
  // -----------

  private val actionsStream: Observer[LedgerActorMessage.PerformQuery[S]] with Observable[LedgerActorMessage.PerformQuery[S]] = {
    ConcurrentSubject[LedgerActorMessage.PerformQuery[S]](MulticastStrategy.replay)
  }

  private[ledger] val ledgerActorStream = {
    Observable
      .apply(
        stateUpdatedEventStream,
        clockSignalsStream,
        actionsStream
      )
      .merge
      .oneach ( processActorMessage )
  }

}

object Ledger {

  /**

      Generates an instance of Ledger with all its external dependencies properly injected. To
      generate an instance of Ledger and specify the external dependencies by hand (for example for
      testing purposes) use the constructor.

    */
  def apply[S, Tx: Codec, Q: Loggable, QR: Loggable](obft: OuroborosBFT[Tx])(
      defaultState: S,
      stateRefreshInterval: FiniteDuration,
      slotDuration: FiniteDuration
  )(processQuery: (S, Q) => QR, transactionExecutor: (S, Tx) => Option[S]): Ledger[S, Tx, Q, QR] = {
    val obftStateGate: StateGate[S, Tx] = obft.StateGate[S](transactionExecutor)
    val storage: StateStorage[S] = new StateStorage[S](defaultState)
    val clockSignalsStream: Observable[LedgerActorMessage.Tick[S]] = {
      Observable
        .intervalWithFixedDelay(delay = stateRefreshInterval, initialDelay = FiniteDuration(0, MILLISECONDS))
        .map { _ =>
          LedgerActorMessage.Tick()
        }
    }
    val stateUpdatedEventStream: Observable[LedgerActorMessage.StateUpdatedEvent[S]] =
      obftStateGate.stateUpdatedEventStream.map(LedgerActorMessage.StateUpdatedEvent.apply)

    new Ledger[S, Tx, Q, QR](obftStateGate, storage, clockSignalsStream, stateUpdatedEventStream)(slotDuration)(processQuery, transactionExecutor)
  }
}
