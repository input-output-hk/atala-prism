package io.iohk.cef.test
import io.iohk.cef.network.MessageStream
import io.iohk.cef.utils.concurrent.CancellableFuture
import monix.execution.schedulers.TestScheduler
import monix.reactive.Observable

class DummyMessageStream[T](val o: Observable[T], scheduler: TestScheduler) extends MessageStream[T] {

  type S[A] = DummyMessageStream[A]
  implicit val s = scheduler

  override def map[U](f: T => U): DummyMessageStream[U] =
    new DummyMessageStream(o.map(f), scheduler)

  override def filter(p: T => Boolean): DummyMessageStream[T] =
    new DummyMessageStream(o.filter(p), scheduler)

  override def fold[U](zero: U)(f: (U, T) => U): CancellableFuture[U] =
    CancellableFuture(o.foldLeftL(zero)(f).runAsync)

  override def foreach(f: T => Unit): CancellableFuture[Unit] =
    CancellableFuture(o.foreach(f))
}
