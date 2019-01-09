package io.iohk.cef.test
import io.iohk.cef.network.MessageStream
import io.iohk.cef.utils.concurrent.CancellableFuture
import monix.execution.Scheduler
import monix.reactive.Observable

import scala.concurrent.duration.FiniteDuration

class DummyMessageStream[T](val o: Observable[T])(implicit scheduler: Scheduler) extends MessageStream[T] {

  type S[A] = DummyMessageStream[A]

  override def map[U](f: T => U): DummyMessageStream[U] =
    new DummyMessageStream(o.map(f))

  override def filter(p: T => Boolean): DummyMessageStream[T] =
    new DummyMessageStream(o.filter(p))

  override def fold[U](zero: U)(f: (U, T) => U): CancellableFuture[U] =
    CancellableFuture(o.foldLeftL(zero)(f).runAsync)

  override def foreach(f: T => Unit): CancellableFuture[Unit] =
    CancellableFuture(o.foreach(f))

  override def prepend(t: T): MessageStream[T] =
    new DummyMessageStream(Observable.cons(t, o))

  override def withTimeout(d: FiniteDuration): MessageStream[T] =
    new DummyMessageStream[T](o.takeByTimespan(d))

  override def take(n: Long): MessageStream[T] = new DummyMessageStream(o.take(n))

  override def takeWhile(predicate: T => Boolean): MessageStream[T] = new DummyMessageStream(o.takeWhile(predicate))
}
