package io.iohk.atala.prism.errors

import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future}

trait ErrorSupport[E <: PrismError, ES <: E with PrismServerError] {
  def logger: Logger

  def wrapAsServerError(cause: Throwable): ES

  implicit class ErrorLoggingOps(error: E) {
    def logWarn(implicit lc: LoggingContext): E = {
      val status = error.toStatus
      logger.warn(s"Issuing ${error.getClass.getSimpleName}: ${status.getCode} ${status.getDescription} ($lc)")
      error
    }
  }

  implicit class InternalServerErrorLoggingOps[T <: E with PrismServerError](error: T) {
    def logErr(implicit lc: LoggingContext): T = {
      logger.error(s"Issuing ${error.getClass.getSimpleName} ($lc)", error.cause)
      error
    }
  }

  implicit class FutureEitherErrorOps[T](v: FutureEither[E, T]) {
    def wrapExceptions(implicit ec: ExecutionContext, lc: LoggingContext): FutureEither[E, T] = {
      v.value.recover { case ex => Left(wrapAsServerError(ex).logErr) }.toFutureEither
    }

    def successMap[U](f: T => U)(implicit ec: ExecutionContext): Future[U] = {
      v.value.flatMap {
        case Left(err) => Future.failed(err.toStatus.asRuntimeException())
        case Right(vv) => Future.successful(f(vv))
      }
    }

    def flatten(implicit ec: ExecutionContext): Future[T] = successMap(identity)
  }
}
