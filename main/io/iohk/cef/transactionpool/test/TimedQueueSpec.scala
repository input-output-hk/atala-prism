package io.iohk.cef.transactionpool
import io.iohk.cef.test.TestClock
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

import scala.collection.immutable.Queue
import scala.concurrent.duration._

class TimedQueueSpec extends FlatSpec with MustMatchers with PropertyChecks {

  behavior of "TimedQueue"

  it should "return elements in a LIFO way" in {
    val clock = TestClock()
    forAll { elems: Seq[String] =>
      val queue = TimedQueue[String](clock)
      val defaultDuration = 1 milli
      val fullQueue = elems.foldLeft(queue)((s, e) => s.enqueue(e, defaultDuration))
      elems.foldLeft(fullQueue)((s, e) => {
        val (nextElem, newQueue) = s.dequeue
        nextElem mustBe e
        newQueue
      })
    }
  }

  it should "expire elements automatically" in {
    val clock = TestClock()
    clock.tick
    forAll { (nonExpired: Seq[String], expired: Seq[String]) =>
      val fixture =
        setupTest(clock, nonExpired, expired)
      fixture.fullQueue.size mustBe fixture.shuffledEntries.size //al entries are valid
      clock.tick(1 second)
      fixture.fullQueue.size mustBe fixture.expectedRemaining.size //entries are expired
      fixture.expectedRemaining.foldLeft(fixture.fullQueue)((s, e) => {
        val (nextElem, newQueue) = s.dequeue
        nextElem mustBe e._1
        newQueue
      })
    }
  }

  it should "calculate size for non expired elements" in {
    val clock = TestClock()
    clock.tick
    forAll { (nonExpired: Seq[String], expired: Seq[String]) =>
      val fixture =
        setupTest(clock, nonExpired, expired)
      clock.tick(1 second)
      fixture.fullQueue.size mustBe fixture.expectedRemaining.size //entries are expired
    }
  }

  it should "calculate filter for non expired elements" in {
    val clock = TestClock()
    clock.tick
    forAll { (nonExpired: Seq[String], expired: Seq[String]) =>
      val fixture =
        setupTest(clock, nonExpired, expired)
      clock.tick(1 second)
      val (maintainEntries, _) = fixture.shuffledEntries.splitAt(fixture.shuffledEntries.size / 2)
      val maintainSet = maintainEntries.map(_._1).toSet
      val resultingTimedQueue = fixture.fullQueue.filter(elem => maintainSet.contains(elem))
      resultingTimedQueue.queue mustBe fixture.expectedRemaining.map(_._1).filter(maintainSet.contains)
    }
  }

  it should "calculate filterNot for non expired elements" in {
    val clock = TestClock()
    clock.tick
    forAll { (nonExpired: Seq[String], expired: Seq[String]) =>
      val fixture =
        setupTest(clock, nonExpired, expired)
      clock.tick(1 second)
      val (removeEntries, _) = fixture.shuffledEntries.splitAt(fixture.shuffledEntries.size / 2)
      val removeSet = removeEntries.map(_._1).toSet
      val resultingTimedQueue = fixture.fullQueue.filterNot(elem => removeSet.contains(elem))
      resultingTimedQueue.queue mustBe fixture.expectedRemaining.map(_._1).filterNot(removeSet.contains)
    }
  }

  it should "calculate isEmpty for non expired elements" in {
    val clock = TestClock()
    forAll { (elems: Seq[String]) =>
      val queue = TimedQueue(clock, Queue(elems.map(elem => (elem, clock.instant().plusSeconds(1))): _*))
      queue.isEmpty mustBe elems.isEmpty
      clock.tick(1 second)
      queue.isEmpty mustBe true
      clock.tick(1 second)
      queue.isEmpty mustBe true
    }
  }

  it should "calculate foreach for non expired elements" in {
    val clock = TestClock()
    forAll { (nonExpired: Seq[String], expired: Seq[String]) =>
      val fixture =
        setupTest(clock, nonExpired, expired)
      clock.tick(1 second)
      var resultingQueue = Queue[String]()
      fixture.fullQueue.foreach(elem => {
        resultingQueue = resultingQueue.enqueue(elem)
      })
      resultingQueue mustBe fixture.expectedRemaining.map(_._1)
    }
  }

  it should "calculate foldLeft for non expired elements" in {
    val clock = TestClock()
    forAll { (nonExpired: Seq[String], expired: Seq[String]) =>
      val fixture =
        setupTest(clock, nonExpired, expired)
      clock.tick(1 second)
      val resultingQueue =
        fixture.fullQueue.foldLeft(Queue[String]())((s, e) => {
          s.enqueue(e)
        })
      resultingQueue mustBe fixture.expectedRemaining.map(_._1)
    }
  }

  private def setupTest(clock: TestClock, nonExpired: Seq[String], expired: Seq[String]) = {
    val expiredDuration = 1 seconds
    val nonExpiredDuration = 2 seconds
    val nonExpiredEntries = nonExpired.map(s => (s, nonExpiredDuration))
    val expiredEntries = expired.map(s => (s, expiredDuration))
    val shuffledEntries = scala.util.Random.shuffle(expiredEntries ++ nonExpiredEntries)
    val timedQueue = TimedQueue[String](clock)
    val shuffledFullQueue = shuffledEntries.foldLeft(timedQueue)((s, e) => s.enqueue(e._1, e._2))
    val expectedRemaining = shuffledEntries diff expiredEntries
    TestFixture(shuffledEntries, shuffledFullQueue, expectedRemaining)
  }

  case class TestFixture(
      shuffledEntries: Seq[(String, FiniteDuration)],
      fullQueue: TimedQueue[String],
      expectedRemaining: Seq[(String, FiniteDuration)]
  )
}
