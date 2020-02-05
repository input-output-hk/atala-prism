package io.iohk.cvp.admin

import io.grpc.Status

object Errors {

  sealed trait AdminError {
    def toStatus: Status
  }

  object AdminError {

    /**
      * Error indicating any kind of Database problem
      *
      * @param databaseReportedError any error information bubbling up from the database
      */
    case class DatabaseError(databaseReportedError: Throwable) extends AdminError {
      override def toStatus: Status = {
        Status.INTERNAL.withDescription(s"Database error: $databaseReportedError")
      }
    }
  }
}
