package io.iohk.atala.prism.errors

import io.iohk.atala.prism.metrics.RequestMeasureUtil
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.slf4j.Logger
import cats.syntax.either._

import scala.concurrent.{ExecutionContext, Future}

trait ErrorSupport[E <: PrismError] {
  def logger: Logger

  def wrapAsServerError(cause: Throwable): E

  def invalidRequest(message: String): E

  protected def respondWith[T](
      request: scalapb.GeneratedMessage,
      error: E,
      serviceName: String,
      methodName: String
  )(implicit
      ec: ExecutionContext
  ): Future[T] = {
    implicit val loggingContext: LoggingContext = LoggingContext(
      "request" -> request
    )
    Future
      .successful(Left(error))
      .toFutureEither
      .wrapAndRegisterExceptions(serviceName, methodName)
      .flatten
  }

  implicit class ErrorLoggingOps(error: E) {
    def logWarn(implicit lc: LoggingContext): E = {
      val status = error.toStatus
      logger.warn(
        s"Issuing ${error.getClass.getSimpleName}: ${status.getCode} ${status.getDescription} ($lc)"
      )
      error
    }
  }

  implicit class InternalServerErrorLoggingOps[T <: PrismServerError](
      error: T
  ) {
    def logErr(implicit lc: LoggingContext): T = {
      logger.error(
        s"Issuing ${error.getClass.getSimpleName} ($lc)",
        error.cause
      )
      error
    }
  }

  implicit class FutureEitherErrorOps[T](v: FutureEither[PrismError, T]) {
    def wrapAndRegisterExceptions(serviceName: String, methodName: String)(implicit
        ec: ExecutionContext,
        lc: LoggingContext
    ): FutureEither[PrismError, T] = {
      def logAndRegisterIfPrismServerError(error: PrismError): PrismError = {
        error match {
          case serverError: PrismServerError =>
            serverError.logErr
            RequestMeasureUtil.increaseErrorCounter(
              serviceName,
              methodName,
              error.toStatus.getCode.value()
            )
            error
          case _ => error
        }
      }

      v.value
        .recover { case ex => wrapAsServerError(ex).asLeft }
        .toFutureEither
        .mapLeft(logAndRegisterIfPrismServerError)
    }

    def successMap[U](f: T => U)(implicit ec: ExecutionContext): Future[U] = {
      v.value.flatMap {
        case Left(err) => Future.failed(err.toStatus.asRuntimeException())
        case Right(vv) => Future.successful(f(vv))
      }
    }

    def successMapWithErrorCounter[R](
        serviceName: String,
        methodName: String,
        f: T => R
    )(implicit
        ec: ExecutionContext
    ): Future[R] = {
      v.value.flatMap {
        case Left(err) =>
          val statusError = err.toStatus
          RequestMeasureUtil.increaseErrorCounter(
            serviceName,
            methodName,
            statusError.getCode.value()
          )
          Future.failed(statusError.asRuntimeException())
        case Right(vv) =>
          Future.successful(f(vv))
      }
    }

    def flatten(implicit ec: ExecutionContext): Future[T] = successMap(identity)
  }
}
