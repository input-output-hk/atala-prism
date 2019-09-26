package io.iohk.node.utils

import scala.concurrent.{ExecutionContext, Future}

class FutureEither[+E, +A](val value: Future[Either[E, A]]) extends AnyVal {
  def map[B](f: A => B)(implicit ec: ExecutionContext): FutureEither[E, B] = {
    val newFuture = value.map { _.map(f) }
    new FutureEither[E, B](newFuture)
  }

  def flatMap[E2 >: E, B](f: A => FutureEither[E2, B])(implicit ec: ExecutionContext): FutureEither[E2, B] = {
    val newFuture = value.flatMap {
      case Right(a) => f(a).value
      case Left(e) => Future.successful(Left(e))
    }

    new FutureEither[E2, B](newFuture)
  }
}

object FutureEither {
  implicit class FutureEitherOps[E, A](val value: Future[Either[E, A]]) extends AnyVal {
    def toFutureEither: FutureEither[E, A] = new FutureEither[E, A](value)
    def toFutureEither[E2](mapper: E => E2)(implicit ec: ExecutionContext): FutureEither[E2, A] = {
      val newFuture = value.map { either =>
        either.left.map(mapper)
      }
      new FutureEither[E2, A](newFuture)
    }
    def toFutureEither(ex: Exception)(implicit ec: ExecutionContext): FutureEither[Nothing, A] = {
      val newFuture = value.map {
        case Left(_) => throw ex
        case Right(x) => Right(x)
      }

      new FutureEither[Nothing, A](newFuture)
    }
  }

  implicit class FutureOps[A](val value: Future[A]) extends AnyVal {
    def toFutureEither(implicit ec: ExecutionContext): FutureEither[Nothing, A] = {
      val newFuture = value.map { x =>
        Right(x)
      }
      new FutureEither[Nothing, A](newFuture)
    }
  }
}
