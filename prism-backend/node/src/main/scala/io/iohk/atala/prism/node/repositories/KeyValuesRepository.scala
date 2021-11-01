package io.iohk.atala.prism.node.repositories

import cats.{Applicative, Comonad, Functor}
import cats.effect.{BracketThrow, Resource}
import cats.implicits._
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO.KeyValue
import io.iohk.atala.prism.node.repositories.logs.KeyValuesRepositoryLogs
import io.iohk.atala.prism.node.repositories.metrics.KeyValuesRepositoryMetrics
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps

@derive(applyK)
trait KeyValuesRepository[F[_]] {

  /** Updates the value for the given key, inserting it if non-existent.
    */
  def upsert(keyValue: KeyValue): F[Unit]

  /** Updates many values for the given keys atomically, inserting non-existent keys.
    */
  def upsertMany(keyValues: List[KeyValue]): F[Unit]

  /** Gets the value for the given key, set to `None` when non-existent or `NULL` in the database.
    */
  def get(key: String): F[KeyValue]

}

object KeyValuesRepository {
  def apply[F[_]: BracketThrow: TimeMeasureMetric, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[KeyValuesRepository[F]] =
    for {
      serviceLogs <- logs.service[KeyValuesRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, KeyValuesRepository[F]] =
        serviceLogs
      val metrics: KeyValuesRepository[Mid[F, *]] =
        new KeyValuesRepositoryMetrics[F]()
      val logs: KeyValuesRepository[Mid[F, *]] = new KeyValuesRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new KeyValuesRepositoryImpl[F](transactor)
    }

  def resource[F[_]: BracketThrow: TimeMeasureMetric, R[
      _
  ]: Applicative: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, KeyValuesRepository[F]] =
    Resource.eval(KeyValuesRepository(transactor, logs))

  def unsafe[F[_]: BracketThrow: TimeMeasureMetric, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): KeyValuesRepository[F] = KeyValuesRepository(transactor, logs).extract
}

private final class KeyValuesRepositoryImpl[F[_]: BracketThrow](
    xa: Transactor[F]
) extends KeyValuesRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  /** Updates the value for the given key, inserting it if non-existent.
    */
  def upsert(keyValue: KeyValue): F[Unit] = {
    KeyValuesDAO
      .upsert(keyValue)
      .logSQLErrors("upserting", logger)
      .transact(xa)
  }

  /** Updates many values for the given keys atomically, inserting non-existent keys.
    */
  def upsertMany(keyValues: List[KeyValue]): F[Unit] = {
    keyValues
      .map(KeyValuesDAO.upsert)
      .sequence
      .logSQLErrors(s"upserting: ${keyValues}", logger)
      .transact(xa)
      .void
  }

  /** Gets the value for the given key, set to `None` when non-existent or `NULL` in the database.
    */
  def get(key: String): F[KeyValue] = {
    KeyValuesDAO
      .get(key)
      .logSQLErrors(s"getting, key - $key", logger)
      .transact(xa)
      .map(_.getOrElse(KeyValue(key, None)))
  }
}
