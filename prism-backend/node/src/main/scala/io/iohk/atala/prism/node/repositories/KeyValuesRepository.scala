package io.iohk.atala.prism.node.repositories

import cats.effect.BracketThrow
import cats.implicits._
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO.KeyValue
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid

@derive(applyK)
trait KeyValuesRepository[F[_]] {

  /**
    * Updates the value for the given key, inserting it if non-existent.
    */
  def upsert(keyValue: KeyValue): F[Unit]

  /**
    * Updates many values for the given keys atomically, inserting non-existent keys.
    */
  def upsertMany(keyValues: List[KeyValue]): F[Unit]

  /**
    * Gets the value for the given key, set to `None` when non-existent or `NULL` in the database.
    */
  def get(key: String): F[KeyValue]

}

object KeyValuesRepository {
  def apply[F[_]: TimeMeasureMetric: BracketThrow](transactor: Transactor[F]): KeyValuesRepository[F] = {
    val metrics: KeyValuesRepository[Mid[F, *]] = new KeyValuesRepositoryMetrics[F]()
    metrics attach new KeyValuesRepositoryImpl[F](transactor)
  }
}

private final class KeyValuesRepositoryImpl[F[_]: BracketThrow](xa: Transactor[F]) extends KeyValuesRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  /**
    * Updates the value for the given key, inserting it if non-existent.
    */
  def upsert(keyValue: KeyValue): F[Unit] = {
    KeyValuesDAO
      .upsert(keyValue)
      .logSQLErrors("upserting", logger)
      .transact(xa)
  }

  /**
    * Updates many values for the given keys atomically, inserting non-existent keys.
    */
  def upsertMany(keyValues: List[KeyValue]): F[Unit] = {
    keyValues
      .map(KeyValuesDAO.upsert)
      .sequence
      .logSQLErrors(s"upserting: ${keyValues}", logger)
      .transact(xa)
      .void
  }

  /**
    * Gets the value for the given key, set to `None` when non-existent or `NULL` in the database.
    */
  def get(key: String): F[KeyValue] = {
    KeyValuesDAO
      .get(key)
      .logSQLErrors(s"getting, key - $key", logger)
      .transact(xa)
      .map(_.getOrElse(KeyValue(key, None)))
  }
}

private final class KeyValuesRepositoryMetrics[F[_]: TimeMeasureMetric: BracketThrow]
    extends KeyValuesRepository[Mid[F, *]] {

  private val repoName = "KeyValuesRepository"
  private lazy val upsertTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "upsert")
  private lazy val upsertManyTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "upsertMany")
  private lazy val getTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "get")

  override def upsert(keyValue: KeyValue): Mid[F, Unit] = _.measureOperationTime(upsertTimer)

  override def upsertMany(keyValues: List[KeyValue]): Mid[F, Unit] = _.measureOperationTime(upsertManyTimer)

  override def get(key: String): Mid[F, KeyValue] = _.measureOperationTime(getTimer)
}
