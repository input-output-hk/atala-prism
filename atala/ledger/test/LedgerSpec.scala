package atala.ledger
package test

import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import org.scalatest.OptionValues._
import atala.clock._

import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive._

import scala.concurrent.duration._
import scala.concurrent.Await
import atala.helpers.monixhelpers._
import atala.obft.StateGate

class LedgerSpec extends WordSpec with MustMatchers {

  "the Ledger" should {

    type S = String
    type Tx = Int

    "be able to query data from the ledger" in {
      val clockSignalsStream: Observable[LedgerActorMessage.Tick[S]] =
        fakeStream
      val stateUpdatedEventStream: Observable[LedgerActorMessage.StateUpdatedEvent[S]] =
        fakeStream
      val storage = fakeStorage[S]("Foo Bar")
      val ledger = new Ledger[S, Tx, Char, Boolean](null, storage, clockSignalsStream, stateUpdatedEventStream)(
        20.millis
      )(_.contains(_), null)
      ledger.run()

      val r1 = Await.result(ledger.ask('F'), 5.seconds)
      r1 mustBe true

      val r2 = Await.result(ledger.ask('K'), 5.seconds)
      r2 mustBe false
    }

    "update it's internal state representation when a new StateUpdatedEvent arrives" in {
      val clockSignalsStream: Observable[LedgerActorMessage.Tick[S]] =
        fakeStream
      val stateUpdatedEventStream =
        fakeStream[LedgerActorMessage.StateUpdatedEvent[S]]
      val storage = fakeStorage[S]("Foo Bar")
      val ledger = new Ledger[S, Tx, Char, Boolean](null, storage, clockSignalsStream, stateUpdatedEventStream)(
        20.millis
      )(_.contains(_), null)
      ledger.run()

      val r1 = Await.result(ledger.ask('K'), 5.seconds)
      r1 mustBe false
      stateUpdatedEventStream.feedItem(
        LedgerActorMessage.StateUpdatedEvent[S](StateSnapshot("OK", TimeSlot.from(10).value))
      )
      Thread.sleep(100)
      val r2 = Await.result(ledger.ask('K'), 5.seconds)
      r2 mustBe true
    }

    "request an state update from obft when a tick arrives" in {
      val clockSignalsStream =
        fakeStream[LedgerActorMessage.Tick[S]]
      val stateUpdatedEventStream =
        fakeStream[LedgerActorMessage.StateUpdatedEvent[S]]
      val storage = fakeStorage[S]("Foo Bar")
      val stateGate = new FakeStateGate[S, Tx]()
      val ledger = new Ledger[S, Tx, Char, Boolean](stateGate, storage, clockSignalsStream, stateUpdatedEventStream)(
        20.millis
      )(_.contains(_), null)
      ledger.run()

      val r1 = stateGate.counter
      r1 mustBe 0
      clockSignalsStream.feedItem(
        LedgerActorMessage.Tick[S]()
      )
      Thread.sleep(100)
      val r2 = stateGate.counter
      r2 mustBe 1
    }
  }

  def fakeStorage[S](s: S): StateStorage[S] = new StateStorage[S](s)

  def fakeStream[T]: Observer[T] with Observable[T] = {
    ConcurrentSubject[T](MulticastStrategy.replay)
  }

  class FakeStateGate[S, Tx] extends StateGate[S, Tx](null, null)(null, true) {
    var counter = 0
    override def requestStateUpdate(now: TimeSlot, previousSnapshot: StateSnapshot[S]): Unit = {
      counter += 1
    }
  }
}
