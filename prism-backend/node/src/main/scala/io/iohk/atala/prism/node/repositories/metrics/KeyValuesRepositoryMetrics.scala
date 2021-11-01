package io.iohk.atala.prism.node.repositories.metrics

import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.node.repositories.KeyValuesRepository
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO.KeyValue
import tofu.higherKind.Mid
import cats.effect.MonadCancelThrow

private[repositories] final class KeyValuesRepositoryMetrics[F[
    _
]: TimeMeasureMetric: MonadCancelThrow]
    extends KeyValuesRepository[Mid[F, *]] {

  private val repoName = "KeyValuesRepository"
  private lazy val upsertTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "upsert")
  private lazy val upsertManyTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "upsertMany")
  private lazy val getTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "get")

  override def upsert(keyValue: KeyValue): Mid[F, Unit] =
    _.measureOperationTime(upsertTimer)

  override def upsertMany(keyValues: List[KeyValue]): Mid[F, Unit] =
    _.measureOperationTime(upsertManyTimer)

  override def get(key: String): Mid[F, KeyValue] =
    _.measureOperationTime(getTimer)
}
