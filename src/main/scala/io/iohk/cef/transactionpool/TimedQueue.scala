package io.iohk.cef.transactionpool
import java.time.{Clock, Duration, Instant}

import scala.collection.immutable.Queue
import scala.concurrent.duration.FiniteDuration

/**
  * An immutable queue that automatically expires entries after a certain duration.
  * @param clock The clock utilized to calculate expiration dates
  * @param q the underlying queue
  * @tparam T the type this queue is supposed to store
  */
case class TimedQueue[T](clock: Clock = Clock.systemUTC(), q: Queue[(T, Instant)] = Queue()) {

  /**
    * Queues an element
    * @param t the element to be queued
    * @param duration how long would it take for this element to become expired
    * @return a new TimedQueue with the element enqueued
    */
  def enqueue(t: T, duration: Duration): TimedQueue[T] = {
    enqueue(t, clock.instant().plus(duration))
  }

  /**
    * Queues an element
    * @param t the element to be queued
    * @param duration how long would it take for this element to become expired
    * @return a new TimedQueue with the element enqueued
    */
  def enqueue(t: T, duration: scala.concurrent.duration.Duration): TimedQueue[T] = {
    enqueue(t, Duration.ofNanos(duration.toNanos))
  }

  /**
    * Queues an element
    * @param t the element to be queued
    * @param until when does this element expire
    * @return a new TimedQueue with the element enqueued
    */
  def enqueue(t: T, until: Instant): TimedQueue[T] = {
    new TimedQueue[T](clock, cleanedQuery.enqueue((t, until)))
  }

  /**
    * Dequeues an unexpired element
    * @return the dequeued element and the resulting queue. If the queue is empty, None is returned.
    */
  def dequeueOption: Option[(T, TimedQueue[T])] = {
    for {
      ((t, _), newQueue) <- cleanedQuery.dequeueOption
    } yield (t, new TimedQueue[T](clock, newQueue))
  }

  /**
    * Dequeues an unexpired element
    * @return the dequeued element and the resulting queue.
    */
  def dequeue: (T, TimedQueue[T]) =
    dequeueOption.getOrElse(throw new IllegalStateException("No more elements in the queue"))

  /**
    * Returns a new queue with only unexpired elements that satisfy the predicate
    * @param predicate
    * @return
    */
  def filter(predicate: T => Boolean): TimedQueue[T] =
    new TimedQueue[T](clock, cleanedQuery.filter(value => predicate(value._1)))

  /**
    * Returns a new queue with only unexpired elements that do not satisfy the predicate.
    * @param predicate
    * @return
    */
  def filterNot(predicate: T => Boolean): TimedQueue[T] =
    new TimedQueue[T](clock, cleanedQuery.filterNot(value => predicate(value._1)))

  /**
    * Executes the function f for each unexpired element.
    * @param f
    */
  def foreach(f: T => Unit): Unit = removeExpired.foreach { case (t, _) => f(t) }

  /**
    * Executes a foldLeft on all unexpired elements and returns the resulting state.
    * @param state
    * @param f
    * @tparam S
    * @return
    */
  def foldLeft[S](state: S)(f: (S, T) => S): S =
    cleanedQuery.foldLeft(state)((s, t) => f(s, t._1))

  def isEmpty: Boolean = cleanedQuery.isEmpty

  /**
    * returns the underlying immutable queue of elements T.
    * @return
    */
  def queue: Queue[T] = toQueue(removeExpired).map(_._1)

  def size: Int = cleanedQuery.size

  private def cleanedQuery: Queue[(T, Instant)] = removeExpired(q)

  private def removeExpired(q: Queue[(T, Instant)]): Queue[(T, Instant)] = {
    val now = clock.instant()
    q.filter(_._2.isAfter(now))
  }
}

object TimedQueue {
  def apply[T](clock: Clock, q: Queue[(T, FiniteDuration)])(implicit dummyImplicit: DummyImplicit): TimedQueue[T] = {
    val now = clock.instant()
    val getExpiration = (f: FiniteDuration) => now.plusNanos(f.toNanos)
    new TimedQueue[T](clock, q.map { case (t, duration) => (t, getExpiration(duration)) })
  }
}
