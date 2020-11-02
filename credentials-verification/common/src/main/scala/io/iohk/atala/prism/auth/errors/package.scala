package io.iohk.atala.prism.auth

import io.grpc.Status
import io.iohk.atala.prism.utils.FutureEither
import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future}

package object errors {
  sealed trait AuthError {
    def toStatus: Status
  }

  final case class SignatureVerificationError() extends AuthError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription("Signature Invalid")
    }
  }

  final case class UnknownPublicKeyId() extends AuthError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription("Unknown public key id")
    }
  }

  case object CanonicalSuffixMatchStateError extends AuthError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription("DID canonical suffix does not match the state content")
    }
  }

  case object InvalidAtalaOperationError extends AuthError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription("Invalid encoded Atala operation")
    }
  }

  case object NoCreateDidOperationError extends AuthError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription("Encoded operation does not create a fresh DID")
    }
  }

  final case class UnsupportedAuthMethod() extends AuthError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription("Unsupported auth method")
    }
  }

  final case class UnexpectedError(status: Status) extends AuthError {
    override def toStatus: Status = status
  }

  trait ErrorSupport {
    def logger: Logger

    implicit class FutureEitherErrorOps[T](v: FutureEither[AuthError, T]) {
      def successMap[U](f: T => U)(implicit ec: ExecutionContext): Future[U] = {
        v.value.flatMap {
          case Left(err: AuthError) => Future.failed(err.toStatus.asRuntimeException())
          case Right(vv) => Future.successful(f(vv))
        }
      }
    }
  }
}
