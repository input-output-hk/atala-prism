package io.iohk.atala.prism.vault.repositories

import cats.effect.Resource
import cats.effect.MonadCancel
import cats.effect.MonadCancelThrow
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.comonad._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{Applicative, Comonad, Functor, MonadThrow}
import derevo.derive
import derevo.tagless.applyK
import doobie.Transactor
import doobie.implicits._
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.vault.model.{CreateRecord, Record}
import io.iohk.atala.prism.vault.repositories.daos.RecordsDAO
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.logging._
import tofu.syntax.monoid.TofuSemigroupOps

@derive(applyK)
trait RecordsRepository[F[_]] {
  def create(recordData: CreateRecord): F[Record]

  def getRecord(type_ : Record.Type, id: Record.Id): F[Option[Record]]

  def getRecordsPaginated(
      type_ : Record.Type,
      lastSeenId: Option[Record.Id],
      limit: Int
  ): F[List[Record]]
}

object RecordsRepository {
  def create[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Functor](
      xa: Transactor[F],
      logs: Logs[R, F]
  ): R[RecordsRepository[F]] =
    for {
      serviceLogs <- logs.service[RecordsRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, RecordsRepository[F]] = serviceLogs
      val metrics: RecordsRepository[Mid[F, *]] = new RecordsRepoMetrics
      val logging: RecordsRepository[Mid[F, *]] = new RecordsRepoLogging
      val mid = metrics |+| logging
      mid attach new RecordsRepositoryImpl(xa)
    }

  def resource[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Applicative: Functor](
      xa: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, RecordsRepository[F]] = Resource.eval(RecordsRepository.create(xa, logs))

  def unsafe[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Comonad](
      xa: Transactor[F],
      logs: Logs[R, F]
  ): RecordsRepository[F] = RecordsRepository.create(xa, logs).extract
}

private class RecordsRepositoryImpl[F[_]](xa: Transactor[F])(implicit
    br: MonadCancel[F, Throwable]
) extends RecordsRepository[F] {

  override def create(recordData: CreateRecord): F[Record] =
    RecordsDAO
      .createRecord(recordData)
      .logSQLErrorsV2("creating")
      .transact(xa)

  override def getRecord(type_ : Record.Type, id: Record.Id): F[Option[Record]] =
    RecordsDAO
      .getRecord(type_, id)
      .logSQLErrorsV2("getting")
      .transact(xa)

  override def getRecordsPaginated(
      type_ : Record.Type,
      lastSeenIdOpt: Option[Record.Id],
      limit: Int
  ): F[List[Record]] =
    RecordsDAO
      .getRecordsPaginated(type_, lastSeenIdOpt, limit)
      .logSQLErrorsV2("getting by paginated")
      .transact(xa)
}

private final class RecordsRepoMetrics[F[_]: TimeMeasureMetric: MonadCancelThrow] extends RecordsRepository[Mid[F, *]] {
  val repoName: String = "records-repository"

  private lazy val createTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "create")

  private lazy val getRecordTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getRecord")

  private lazy val getRecordsPaginatedTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getRecordsPaginated")

  override def create(recordData: CreateRecord): Mid[F, Record] =
    _.measureOperationTime(createTimer)

  override def getRecord(type_ : Record.Type, id: Record.Id): Mid[F, Option[Record]] =
    _.measureOperationTime(getRecordTimer)

  override def getRecordsPaginated(
      type_ : Record.Type,
      lastSeenIdOpt: Option[Record.Id],
      limit: Int
  ): Mid[F, List[Record]] =
    _.measureOperationTime(getRecordsPaginatedTimer)
}

private final class RecordsRepoLogging[
    F[_]: MonadThrow: ServiceLogging[*[_], RecordsRepository[F]]
] extends RecordsRepository[Mid[F, *]] {
  override def create(recordData: CreateRecord): Mid[F, Record] =
    in =>
      info"creating record ${recordData.id}" *> in
        .flatTap(r => info"creating record - successfully done ${r.id}")
        .onError(e => errorCause"an error occurred while creating record" (e))

  override def getRecord(type_ : Record.Type, id: Record.Id): Mid[F, Option[Record]] =
    in =>
      info"getting record type = ${type_.toString}, id = ${id.toString}" *> in
        .flatTap(_ => info"getting record - successfully found")
        .onError(e => errorCause"an error occurred while getting record" (e))

  override def getRecordsPaginated(
      type_ : Record.Type,
      lastSeenIdOpt: Option[Record.Id],
      limit: Int
  ): Mid[F, List[Record]] =
    in =>
      info"getting paginated data ${type_.toString} {limit=$limit}" *> in
        .flatTap(r => info"getting paginated data - successfully done got ${r.size} entities")
        .onError(e => errorCause"an error occurred while getting paginated records" (e))
}
