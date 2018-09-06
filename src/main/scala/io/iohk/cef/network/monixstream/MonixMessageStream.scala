package io.iohk.cef.network.monixstream

import io.iohk.cef.network.MessageStream
import monix.execution.Scheduler
import monix.reactive.Observable
import scala.concurrent.Future

private[network] class MonixMessageStream[T](o: Observable[T]) extends MessageStream[T] {

  implicit val monixScheduler: Scheduler = monix.execution.Scheduler.Implicits.global

  override def map[U](f: T => U): MessageStream[U] =
    new MonixMessageStream(o.map(f))

  override def filter(p: T => Boolean): MessageStream[T] =
    new MonixMessageStream(o.filter(p))

  override def fold[U](zero: U)(f: (U, T) => U): Future[U] =
    o.foldLeftL(zero)(f).runAsync

  override def foreach(f: T => Unit): Future[Unit] =
    o.foreach(f)
}
