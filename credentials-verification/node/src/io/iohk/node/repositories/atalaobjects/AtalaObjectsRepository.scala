package io.iohk.node.repositories.atalaobjects

import cats.effect.IO
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps
import io.iohk.node.repositories.daos.AtalaObjectsDAO

import scala.concurrent.ExecutionContext

class AtalaObjectsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  def createReference(
      data: AtalaObjectsDAO.AtalaObjectCreateData
  ): FutureEither[Nothing, Unit] = {
    AtalaObjectsDAO.insert(data).runQuery
  }

  private implicit class ConnectionIOExtensions[T](val query: ConnectionIO[T]) {
    def runQuery: FutureEither[Nothing, Unit] =
      query
        .transact(xa)
        .map(_ => ())
        .unsafeToFuture()
        .map(Right.apply)
        .toFutureEither
  }

}
