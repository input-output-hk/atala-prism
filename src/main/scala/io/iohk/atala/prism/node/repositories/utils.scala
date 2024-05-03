package io.iohk.atala.prism.node.repositories

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.node.errors.NodeError

import java.sql.SQLException

object utils {
  def dbErrorWrapper[T](
      resultE: Either[SQLException, T]
  ): Either[NodeError, T] =
    resultE.left.map(NodeError.InternalErrorDB)

  def connectionIOSafe[T](
      connectionIO: ConnectionIO[T]
  ): ConnectionIO[Either[NodeError, T]] =
    connectionIO.attemptSql
      .map(dbErrorWrapper)
}
