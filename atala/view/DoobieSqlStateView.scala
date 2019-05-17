package atala.view

import doobie._
import doobie.implicits._
import cats.implicits._
import atala.obft.OuroborosBFT
import atala.obft.common.StateGate
import monix.reactive._
import atala.clock._
import atala.obft.common.StateSnapshot

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import atala.logging._
import cats.data.State

trait DoobieFutureExecutor {
  def apply[T](c: ConnectionIO[T]): Future[T]
}

class DoobieSqlStateView[Tx, Q: Loggable, QR: Loggable](
    override protected val obftStateGate: StateGate[State[ConnectionIO[Int], Unit]],
    override protected val clockSignalsStream: Observable[StateViewActorMessage.Tick[State[ConnectionIO[Int], Unit]]],
    override protected val stateUpdatedEventStream: Observable[
      StateViewActorMessage.StateUpdatedEvent[State[ConnectionIO[Int], Unit]]
    ]
)(override protected val slotDuration: FiniteDuration)(
    execute: DoobieFutureExecutor
)(
    query: Q => ConnectionIO[QR]
) extends StateView[State[ConnectionIO[Int], Unit], Tx, Q, QR] {

  private var currentTimeSlot: TimeSlot = TimeSlot.zero

  override protected final def stateSnapshot: StateSnapshot[State[ConnectionIO[Int], Unit]] =
    StateSnapshot(State.set(0.pure[ConnectionIO]), currentTimeSlot)

  override final protected def lastStateUpdateTimeSlot: TimeSlot = currentTimeSlot
  protected def updateState(newState: StateSnapshot[State[ConnectionIO[Int], Unit]]): Unit = {
    currentTimeSlot = newState.snapshotTimestamp
  }

  protected def runQuery(q: Q): Future[QR] =
    execute(query(q))

}

object DoobieSqlStateView {

  def apply[Tx, Q: Loggable, QR: Loggable](
      obft: OuroborosBFT[Tx],
      xa: Transactor[Future]
  )(stateRefreshInterval: FiniteDuration, slotDuration: FiniteDuration)(
      transactionExecutor: Tx => ConnectionIO[Int],
      queryExecutor: Q => ConnectionIO[QR]
  )(implicit ec: ExecutionContext): DoobieSqlStateView[Tx, Q, QR] = {

    val execute: DoobieFutureExecutor = new DoobieFutureExecutor {
      override def apply[T](c: ConnectionIO[T]): Future[T] =
        c.transact(xa)
    }

    def prepareExecution(
        now: TimeSlot,
        previousSnapshot: StateSnapshot[State[ConnectionIO[Int], Unit]]
    ): State[ConnectionIO[Int], Unit] = {
      State.set(0.pure[ConnectionIO])
    }

    def realTransactionExecutor(
        state: State[ConnectionIO[Int], Unit],
        transaction: Tx
    ): Option[State[ConnectionIO[Int], Unit]] = Some {
      for {
        accum <- state.get
        compose = for (a <- accum; t <- transactionExecutor(transaction)) yield a + t
        _ <- State.set(compose)
      } yield ()
    }

    def finalizeExecution(sqlProgram: StateSnapshot[State[ConnectionIO[Int], Unit]]): Unit = {
      val execution = for { p <- sqlProgram.computedState.get } yield execute(p)
      val initialState: ConnectionIO[Int] = 0.pure[ConnectionIO]
      execution.run(initialState)
    }

    val obftStateGate: StateGate[State[ConnectionIO[Int], Unit]] = {
      obft.gate(prepareExecution, realTransactionExecutor, finalizeExecution)
    }

    val clockSignalsStream: Observable[StateViewActorMessage.Tick[State[ConnectionIO[Int], Unit]]] = {
      Observable
        .intervalWithFixedDelay(delay = stateRefreshInterval, initialDelay = FiniteDuration(0, MILLISECONDS))
        .map { _ =>
          StateViewActorMessage.Tick()
        }
    }
    val stateUpdatedEventStream: Observable[StateViewActorMessage.StateUpdatedEvent[State[ConnectionIO[Int], Unit]]] =
      obftStateGate.stateUpdatedEventStream.map(StateViewActorMessage.StateUpdatedEvent.apply)

    new DoobieSqlStateView(obftStateGate, clockSignalsStream, stateUpdatedEventStream)(slotDuration)(execute)(
      queryExecutor
    )
  }

}
