package io.iohk.atala.prism.errors

import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future}

trait ErrorSupport[E <: PrismError] {
  def logger: Logger

  def wrapAsServerError(cause: Throwable): E

  def invalidRequest(message: String): E

  protected def respondWith[T](request: scalapb.GeneratedMessage, error: E)(implicit
      ec: ExecutionContext
  ): Future[T] = {
    implicit val loggingContext: LoggingContext = LoggingContext("request" -> request)
    Future.successful(Left(error)).toFutureEither.wrapExceptions.flatten
  }

  implicit class ErrorLoggingOps(error: E) {
    def logWarn(implicit lc: LoggingContext): E = {
      val status = error.toStatus
      logger.warn(s"Issuing ${error.getClass.getSimpleName}: ${status.getCode} ${status.getDescription} ($lc)")
      error
    }
  }

  implicit class InternalServerErrorLoggingOps[T <: PrismServerError](error: T) {
    def logErr(implicit lc: LoggingContext): T = {
      logger.error(s"Issuing ${error.getClass.getSimpleName} ($lc)", error.cause)
      error
    }
  }

  implicit class FutureEitherErrorOps[T](v: FutureEither[E, T]) {
    def wrapExceptions(implicit ec: ExecutionContext, lc: LoggingContext): FutureEither[E, T] = {
      def logIfPrismServerError(error: E): E = {
        error match {
          case serverError: PrismServerError =>
            serverError.logErr
            error
          case _ => error
        }
      }

      v.value
        .recover { case ex => Left(wrapAsServerError(ex)) }
        .toFutureEither
        .mapLeft(logIfPrismServerError)
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
