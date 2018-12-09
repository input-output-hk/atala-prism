package io.iohk.cef.network

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

trait MessageStream[T] {
  type S[A] <: MessageStream[A]

  def map[U](f: T => U): S[U]
  def filter(p: T => Boolean): S[T]
  def fold[U](zero: U)(f: (U, T) => U): Future[U]
  def foreach(f: T => Unit): Future[Unit]
  def prepend(t: T): MessageStream[T]
  def withTimeout(d: FiniteDuration): MessageStream[T]
}
