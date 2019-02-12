package io.iohk.cef.frontend
import io.iohk.cef.error.ApplicationError

import scala.concurrent.{ExecutionContext, Future}

package object client {

  type Response[T] = Future[Either[ApplicationError, T]]

  case class DoesFor[T](res: Response[T]) {
    def map[U](f: T => U)(implicit ec: ExecutionContext): DoesFor[U] =
      DoesFor(res.map(_.map(f)))
    def flatMap[U](f: T => DoesFor[U])(implicit ec: ExecutionContext): DoesFor[U] =
      DoesFor(
        res.flatMap {
          case Left(e) => Future.successful(Left(e))
          case Right(r) => f(r).res
        }
      )
  }

  implicit class ServiceResponseExtensions[T](val res: Response[T]) extends AnyVal {
    def onFor: DoesFor[T] = DoesFor(res)
  }
}
