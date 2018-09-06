package io.iohk.cef.network

import scala.concurrent.Future

trait MessageStream[T] {
  def map[U](f: T => U): MessageStream[U]
  def filter(p: T => Boolean): MessageStream[T]
  def fold[U](zero: U)(f: (U, T) => U): Future[U]
  def foreach(f: T => Unit): Future[Unit]
}
