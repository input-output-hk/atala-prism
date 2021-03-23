package io.iohk.atala.prism.node.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO.KeyValue
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

class KeyValuesRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  /**
    * Updates the value for the given key, inserting it if non-existent.
    */
  def upsert(keyValue: KeyValue): FutureEither[Nothing, Unit] = {
    KeyValuesDAO
      .upsert(keyValue)
      .logSQLErrors("upserting", logger)
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
      .logSQLErrors(s"getting, key - $key", logger)
      .transact(xa)
      .unsafeToFuture()
      .map {
        case Some(x) => Right(x)
        case None => Right(KeyValue(key, None))
      }
      .toFutureEither
  }
}
