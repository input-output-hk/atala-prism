package io.iohk.cef.utils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.higherKinds

trait ForExpressionsEnabler[F[_]] {
  def enable[A](f: F[A]): ForExpressionsEnabled[F]
}

trait ForExpressionsEnabled[F[_]] {
  type A

  def map[B](f: A => B): F[B]

  def flatMap[B](f: A => F[B]): F[B]

  def pure(a: A): F[A]
}

object ForExpressionsEnabler {
  implicit val futureEnabler: ForExpressionsEnabler[Future] = new ForExpressionsEnabler[Future] {
    override def enable[Value](future: Future[Value]): ForExpressionsEnabled[Future] = new ForExpressionsEnabled[Future] {
      override type A = Value

      override def flatMap[B](f: A => Future[B]): Future[B] = future.flatMap(f)

      override def map[B](f: A => B): Future[B] = future.map(f)

      override def pure(a: A): Future[A] = Future(a)
    }
  }
}

