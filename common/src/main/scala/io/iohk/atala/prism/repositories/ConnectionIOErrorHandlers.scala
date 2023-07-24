package io.iohk.atala.prism.repositories

import cats.syntax.applicativeError._
import doobie.ConnectionIO
import doobie.util.invariant
import org.postgresql.util.PSQLException
import org.slf4j.Logger

object ConnectionIOErrorHandlers {

  def handleSQLErrors[T](
      in: ConnectionIO[T],
      logger: Logger,
      operationDescription: => String,
      returnValue: => ConnectionIO[T]
  ): ConnectionIO[T] =
    in.handleErrorWith {
      case e: invariant.UnexpectedCursorPosition =>
        logger.error(
          s"""Encountered db error while $operationDescription error message - ${e.getMessage}"""
        )
        returnValue
      // 42601 - Syntax error, can occur during development
      case e: PSQLException if e.getSQLState == "42601" =>
        logger.error(
          s"""Encountered SQL syntax error while $operationDescription error message - ${e.getMessage}"""
        )
        returnValue
      // Must be handled by a caller
      case e => e.raiseError[ConnectionIO, T]
    }

}
