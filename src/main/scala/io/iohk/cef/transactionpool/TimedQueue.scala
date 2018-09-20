package io.iohk.cef.transactionpool
import java.time.{Clock, Duration, Instant}

import scala.collection.immutable.Queue
import scala.concurrent.duration.FiniteDuration

case class TimedQueue[T](clock: Clock = Clock.systemUTC(), q: Queue[(T, Instant)] = Queue()) {

  def enqueue(t: T, duration: Duration): TimedQueue[T] = {
    enqueue(t, clock.instant().plus(duration))
  }

  def enqueue(t: T, duration: scala.concurrent.duration.Duration): TimedQueue[T] = {
    enqueue(t, Duration.ofNanos(duration.toNanos))
  }

  def enqueue(t: T, until: Instant): TimedQueue[T] = {
    new TimedQueue[T](clock, cleanedQuery.enqueue((t, until)))
  }

  def dequeueOption: Option[(T, TimedQueue[T])] = {
    for {
      ((t, _), newQueue) <- cleanedQuery.dequeueOption
    } yield (t, new TimedQueue[T](clock, newQueue))
  }

  def dequeue: (T, TimedQueue[T]) =
    dequeueOption.getOrElse(throw new IllegalStateException("No more elements in the queue"))

  def filter(predicate: T => Boolean): TimedQueue[T] =
    new TimedQueue[T](clock, cleanedQuery.filter(value => predicate(value._1)))

  def filterNot(predicate: T => Boolean): TimedQueue[T] =
    new TimedQueue[T](clock, cleanedQuery.filterNot(value => predicate(value._1)))

  def foreach(f: T => Unit): Unit = cleanedQuery.foreach { case (t, _) => f(t) }

  def foldLeft[S](state: S)(f: (S, T) => S): S =
    cleanedQuery.foldLeft(state)((s, t) => f(s, t._1))

  def isEmpty: Boolean = cleanedQuery.isEmpty

  def queue: Queue[T] = cleanedQuery.map(_._1)

  def size: Int = cleanedQuery.size

  private def cleanedQuery: Queue[(T, Instant)] = removeExpired(q)

  private def removeExpired(q: Queue[(T, Instant)]): Queue[(T, Instant)] = {
    val now = clock.instant()
    q.filter(_._2.isAfter(now))
  }
}

object TimedQueue {
  def apply[T](clock: Clock, q: Queue[(T, FiniteDuration)])(implicit dummyImplicit: DummyImplicit): TimedQueue[T] ={
    val now = clock.instant()
    val getExpiration = (f: FiniteDuration) => now.plusNanos(f.toNanos)
    new TimedQueue[T](clock, q.map{ case (t, duration) => (t, getExpiration(duration))})
  }
}
