package io.iohk.atala.prism.management.console.errors

import io.iohk.atala.prism.errors.{ErrorSupport, LoggingContext}
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.{ExecutionContext, Future}

trait ManagementConsoleErrorSupport extends ErrorSupport[ManagementConsoleError, InternalServerError] {
  override def wrapAsServerError(cause: Throwable): InternalServerError =
    InternalServerError(cause)

  protected def respondWith[T](request: scalapb.GeneratedMessage, error: ManagementConsoleError)(implicit
      ec: ExecutionContext
  ): Future[T] = {
    implicit val loggingContext: LoggingContext = LoggingContext("request" -> request)
    Future.successful(Left(error)).toFutureEither.wrapExceptions.flatten
  }
}
