package io.iohk.atala.prism.node.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO.KeyValue

import scala.concurrent.ExecutionContext

class KeyValuesRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  /**
    * Updates the value for the given key, inserting it if non-existent.
    */
  def upsert(keyValue: KeyValue): FutureEither[Nothing, Unit] = {
    KeyValuesDAO
      .upsert(keyValue)
      .transact(xa)
      .unsafeToFuture()
      .map(Right.apply)
      .toFutureEither
  }

  /**
    * Gets the value for the given key, set to `None` when non-existent or `NULL` in the database.
    */
  def get(key: String): FutureEither[Nothing, KeyValue] = {
    KeyValuesDAO
      .get(key)
      .transact(xa)
      .unsafeToFuture()
      .map {
        case Some(x) => Right(x)
        case None => Right(KeyValue(key, None))
      }
      .toFutureEither
  }
}
