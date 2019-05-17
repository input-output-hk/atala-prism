package atala.view
package test

import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import org.scalatest.OptionValues._
import org.scalatest.concurrent.ScalaFutures._
import atala.clock._
import atala.obft.common.StateSnapshot

import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive._

import scala.concurrent.duration._
import atala.helpers.monixhelpers._

class InMemoryStateViewSpec extends WordSpec with MustMatchers {

  "the InMemoryStateView" should {

    type S = String
    type Tx = Int

    "be able to query data from the view" in {
      val clockSignalsStream: Observable[StateViewActorMessage.Tick[S]] =
        fakeStream
      val stateUpdatedEventStream: Observable[StateViewActorMessage.StateUpdatedEvent[S]] =
        fakeStream
      val view = new InMemoryStateView[S, Tx, Char, Boolean](null, clockSignalsStream, stateUpdatedEventStream)(
        "Foo Bar",
        20.millis
      )(_.contains(_), null)
      view.run()

      val r1 = view.ask('F').futureValue
      r1 mustBe true

      val r2 = view.ask('K').futureValue
      r2 mustBe false
    }

    "update it's internal state representation when a new StateUpdatedEvent arrives" in {
      val clockSignalsStream: Observable[StateViewActorMessage.Tick[S]] =
        fakeStream
      val stateUpdatedEventStream =
        fakeStream[StateViewActorMessage.StateUpdatedEvent[S]]
      val view = new InMemoryStateView[S, Tx, Char, Boolean](null, clockSignalsStream, stateUpdatedEventStream)(
        "Foo Bar",
        20.millis
      )(_.contains(_), null)
      view.run()

      val r1 = view.ask('K').futureValue
      r1 mustBe false
      stateUpdatedEventStream.feedItem(
        StateViewActorMessage.StateUpdatedEvent[S](StateSnapshot("OK", TimeSlot.from(10).value))
      )
      Thread.sleep(100)
      val r2 = view.ask('K').futureValue
      r2 mustBe true
    }

    "request an state update from obft when a tick arrives" in {
      val clockSignalsStream =
        fakeStream[StateViewActorMessage.Tick[S]]
      val stateUpdatedEventStream =
        fakeStream[StateViewActorMessage.StateUpdatedEvent[S]]
      val stateGate = new FakeStateGate[S]()
      val view = new InMemoryStateView[S, Tx, Char, Boolean](stateGate, clockSignalsStream, stateUpdatedEventStream)(
        "Foo Bar",
        20.millis
      )(_.contains(_), null)
      view.run()

      val r1 = stateGate.counter
      r1 mustBe 0
      clockSignalsStream.feedItem(
        StateViewActorMessage.Tick[S]()
      )
      Thread.sleep(100)
      val r2 = stateGate.counter
      r2 mustBe 1
    }
  }

  def fakeStream[T]: Observer[T] with Observable[T] = {
    ConcurrentSubject[T](MulticastStrategy.replay)
  }

  class FakeStateGate[S] extends atala.state.StateGate[S, TimeSlot](null)(null, null, null) {
    var counter = 0
    override def requestStateUpdate(now: TimeSlot, previousSnapshot: StateSnapshot[S]): Unit = {
      counter += 1
    }
  }
}
