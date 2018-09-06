package io.iohk.cef.network.transport

import monix.execution.Scheduler
import monix.reactive.Observable

import scala.concurrent.Future
trait StreamLike[T] {
  def map[U](f: T => U): StreamLike[U]
  def fold[U](zero: U)(f: (U, T) => U): Future[U]
  def foreach(f: T => Unit): Future[Unit]
}

class MonixStreamLike[T](o: Observable[T]) extends StreamLike[T] {

  implicit val monixScheduler: Scheduler = monix.execution.Scheduler.Implicits.global

  override def map[U](f: T => U): StreamLike[U] =
    new MonixStreamLike(o.map(f))

  override def fold[U](zero: U)(f: (U, T) => U): Future[U] =
    o.foldLeftL(zero)(f).runAsync

  override def foreach(f: T => Unit): Future[Unit] =
    o.foreach(f)

//  override def start(): Unit =
//    o.subscribe()
}
