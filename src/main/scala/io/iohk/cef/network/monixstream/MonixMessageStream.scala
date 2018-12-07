package io.iohk.cef.network.monixstream

import io.iohk.cef.network.MessageStream
import io.iohk.cef.utils.concurrent.CancellableFuture
import monix.execution.Scheduler
import monix.reactive.Observable

import scala.concurrent.duration.FiniteDuration

private[network] class MonixMessageStream[T](val o: Observable[T]) extends MessageStream[T] {

  type S[A] = MonixMessageStream[A]

  implicit val monixScheduler: Scheduler = monix.execution.Scheduler.Implicits.global

  override def map[U](f: T => U): MonixMessageStream[U] =
    new MonixMessageStream(o.map(f))

  override def filter(p: T => Boolean): MonixMessageStream[T] =
    new MonixMessageStream(o.filter(p))

  override def fold[U](zero: U)(f: (U, T) => U): CancellableFuture[U] =
    CancellableFuture(o.foldLeftL(zero)(f).runAsync)

  override def foreach(f: T => Unit): CancellableFuture[Unit] =
    CancellableFuture(o.foreach(f))

  override def prepend(t: T): MessageStream[T] =
    new MonixMessageStream(Observable.cons(t, o))

  override def withTimeout(
      d: FiniteDuration): MessageStream[T] = {
    new MonixMessageStream[T](o.takeByTimespan(d))
  }
}

object MonixMessageStream {
  def empty[T](): MonixMessageStream[T] = new MonixMessageStream[T](Observable.empty)
}
