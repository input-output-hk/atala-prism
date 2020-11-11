package io.iohk.atala.prism.management.console

import io.grpc.Status
import io.iohk.atala.prism.errors.{PrismError, PrismServerError}

package object errors {
  sealed trait ManagementConsoleError extends PrismError

  case class UnknownValueError(tpe: String, value: String) extends ManagementConsoleError {
    override def toStatus: Status = {
      Status.UNKNOWN.withDescription(s"Unknown $tpe: $value")
    }
  }

  case class InternalServerError(cause: Throwable) extends ManagementConsoleError with PrismServerError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription("Internal server error. Please contact administrator.")
    }
  }
}
