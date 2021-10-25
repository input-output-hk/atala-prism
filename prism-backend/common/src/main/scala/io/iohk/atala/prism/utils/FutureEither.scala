package io.iohk.atala.prism.utils

import cats.Functor

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class FutureEither[+E, +A](val value: Future[Either[E, A]]) extends AnyVal {
  def map[B](f: A => B)(implicit ec: ExecutionContext): FutureEither[E, B] = {
    val newFuture = value.map { _.map(f) }
    new FutureEither[E, B](newFuture)
  }

  def mapLeft[F](
      f: E => F
  )(implicit ec: ExecutionContext): FutureEither[F, A] = {
    val newFuture = value.map { _.left.map(f) }
    new FutureEither[F, A](newFuture)
  }

  def flatMap[E2 >: E, B](
      f: A => FutureEither[E2, B]
  )(implicit ec: ExecutionContext): FutureEither[E2, B] = {
    val newFuture = value.flatMap {
      case Right(a) => f(a).value
      case Left(e) => Future.successful(Left(e))
    }

    new FutureEither[E2, B](newFuture)
  }

  def innerFlatMap[E2 >: E, B](f: A => Either[E2, B])(implicit
      ec: ExecutionContext
  ): FutureEither[E2, B] =
    transform(e => Left(e), f)

  def transform[E2, A2](fa: E => Either[E2, A2], fb: A => Either[E2, A2])(implicit
      ec: ExecutionContext
  ): FutureEither[E2, A2] = {
    val newFuture = value.map {
      case Left(e) => fa(e)
      case Right(x) => fb(x)
    }

    new FutureEither(newFuture)
  }

  def transformWith[E2, A2](
      fa: E => FutureEither[E2, A2],
      fb: A => FutureEither[E2, A2]
  )(implicit
      ec: ExecutionContext
  ): FutureEither[E2, A2] = {

    val newFuture = value.flatMap {
      case Left(e) => fa(e).value
      case Right(x) => fb(x).value
    }

    new FutureEither(newFuture)
  }

  def failOnLeft(
      f: E => Exception
  )(implicit ec: ExecutionContext): FutureEither[Nothing, A] = {
    val newFuture = value.flatMap {
      case Right(x) => Future.successful(Right(x))
      case Left(e) => Future.failed(f(e))
    }

    new FutureEither(newFuture)
  }

  def recoverLeft[A2 >: A](
      f: E => A2
  )(implicit ec: ExecutionContext): FutureEither[Nothing, A2] = {
    new FutureEither(value.map(e => Right(e.fold(f, identity))))
  }

  def toFuture(ef: E => Throwable)(implicit ec: ExecutionContext): Future[A] = {
    value.flatMap(e => e.fold(e => Future.failed(ef(e)), a => Future.successful(a)))
  }

  def toFuture(implicit
      ev: E <:< Throwable,
      ec: ExecutionContext
  ): Future[A] = {
    value.flatMap(e => Future.fromTry(e.toTry))
  }
}

/** NOTE: Avoid defining generic future extensions, like {{{FutureOps[A](val value: Future[A])}}}
  *
  * These could cause ambiguity to the compiler while resolving implicits.
  */
object FutureEither {

  /** Constructs a `FutureEither` from the given `body` by wrapping it in a `Try`.
    *
    * <p>This method ensures any non-fatal exception is caught.
    */
  def apply[E, A](body: => A): FutureEither[Throwable, A] = {
    apply(Try(body))
  }

  /** Constructs a `FutureEither` from the given `Try`.
    */
  def apply[E, A](tryBody: Try[A]): FutureEither[Throwable, A] = {
    Future.successful {
      tryBody match {
        case Failure(ex) => Left(ex)
        case Success(value) => Right(value)
      }
    }.toFutureEither
  }

  def left[E, A](leftValue: E): FutureEither[E, A] =
    Future.successful(Left(leftValue)).toFutureEither

  def right[E, A](rightValue: A): FutureEither[E, A] =
    Future.successful(Right(rightValue)).toFutureEither

  implicit class FutureEitherFOps[A](val value: Future[A]) extends AnyVal {
    /* Lifts a Future[A] to a successful FutureEither
     */
    def lift[E](implicit
        executionContext: ExecutionContext
    ): FutureEither[E, A] = value.map(Right(_)).toFutureEither
  }

  implicit class FutureEitherOps[E, A](val value: Future[Either[E, A]]) extends AnyVal {
    def toFutureEither: FutureEither[E, A] = new FutureEither[E, A](value)
    def toFutureEither[E2](
        mapper: E => E2
    )(implicit ec: ExecutionContext): FutureEither[E2, A] = {
      val newFuture = value.map { either =>
        either.left.map(mapper)
      }
      new FutureEither[E2, A](newFuture)
    }
    def toFutureEither(
        ex: Exception
    )(implicit ec: ExecutionContext): FutureEither[Nothing, A] = {
      val newFuture = value.map {
        case Left(_) => throw ex
        case Right(x) => Right(x)
      }

      new FutureEither[Nothing, A](newFuture)
    }
  }

  implicit class FutureOptionOps[A](val value: Future[Option[A]]) extends AnyVal {
    def toFutureEither[E](
        error: => E
    )(implicit ec: ExecutionContext): FutureEither[E, A] = {
      val newFuture = value.map {
        case Some(x) => Right(x)
        case None => Left(error)
      }
      new FutureEither[E, A](newFuture)
    }
  }

  implicit class EitherOps[E, A](val value: Either[E, A]) extends AnyVal {
    def toFutureEither: FutureEither[E, A] = {
      Future.successful(value).toFutureEither
    }
  }

  implicit def futureEitherFunctor[L](implicit
      ec: ExecutionContext
  ): Functor[FutureEither[L, *]] =
    new Functor[FutureEither[L, *]] {
      override def map[A, B](fa: FutureEither[L, A])(
          f: A => B
      ): FutureEither[L, B] = fa.map(f)
    }

}
