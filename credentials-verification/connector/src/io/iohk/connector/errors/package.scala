package io.iohk.connector

import io.grpc.Status
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future}

package object errors {

  sealed trait ConnectorError {
    def toStatus: Status
  }

  case class UnknownValueError(tpe: String, value: String) extends ConnectorError {
    override def toStatus: Status = {
      Status.UNKNOWN.withDescription(s"Unknown $tpe: $value")
    }
  }

  case class InvalidArgumentError(tpe: String, requirement: String, value: String) extends ConnectorError {
    override def toStatus: Status = {
      Status.INVALID_ARGUMENT.withDescription(s"Invalid value for $tpe, expected $requirement, got $value")
    }
  }

  case class UserIdMissingError() extends ConnectorError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription("Missing UserId")
    }
  }
  case class PublicKeyMissingError() extends ConnectorError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription("Missing Public Key")
    }
  }
  case class SignatureMissingError() extends ConnectorError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription("Missing Signature")
    }
  }
  case class SignatureVerificationError() extends ConnectorError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription("Signature Invalid")
    }
  }

  case class InternalServerError(cause: Throwable) extends ConnectorError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription("Internal server error. Please contact administrator.")
    }
  }

  case class ServiceUnavailableError() extends ConnectorError {
    override def toStatus: Status = {
      Status.UNAVAILABLE.withDescription("Service unavailable. Please try later.")
    }
  }

  case class LoggingContext(context: Map[String, String]) extends AnyVal {
    override def toString: String = {
      context.toList.sorted.map { case (k, v) => s"$k: $v" }.mkString(", ")
    }
  }

  object LoggingContext {
    def apply(pairs: (String, Any)*): LoggingContext = {
      LoggingContext(pairs.map { case (k, v) => (k, v.toString) }.toMap)
    }
  }

  trait ErrorSupport {
    def logger: Logger

    implicit class ErrorLoggingOps[T <: ConnectorError](error: T) {
      def logWarn(implicit lc: LoggingContext): T = {
        val status = error.toStatus
        logger.warn(s"Issuing ${error.getClass.getSimpleName}: ${status.getCode} ${status.getDescription} ($lc)")
        error
      }
    }

    implicit class InternalServerErrorLoggingOps(error: InternalServerError) {
      def logErr(implicit lc: LoggingContext): InternalServerError = {
        logger.error(s"Issuing ${error.getClass.getSimpleName} ($lc)", error.cause)
        error
      }
    }

    implicit class FutureEitherErrorOps[T](v: FutureEither[ConnectorError, T]) {
      def wrapExceptions(implicit ec: ExecutionContext, lc: LoggingContext): FutureEither[ConnectorError, T] = {
        v.value.recover { case ex => Left(InternalServerError(ex).logErr) }.toFutureEither
      }

      def successMap[U](f: T => U)(implicit ec: ExecutionContext): Future[U] = {
        v.value.flatMap {
          case Left(err: ConnectorError) => Future.failed(err.toStatus.asRuntimeException())
          case Right(vv) => Future.successful(f(vv))
        }
      }

      def flatten(implicit ec: ExecutionContext): Future[T] = successMap(identity)
    }
  }
}
