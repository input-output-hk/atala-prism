package io.iohk.atala.prism.vault.repositories

import cats.syntax.apply._
import cats.syntax.comonad._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import doobie.Transactor
import doobie.implicits._
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.vault.model.{CreatePayload, Payload}
import io.iohk.atala.prism.vault.repositories.daos.PayloadsDAO
import derevo.derive
import derevo.tagless.applyK
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps
import tofu.syntax.logging._
import cats.{Applicative, Comonad, Functor, MonadThrow}
import cats.effect.{MonadCancel, MonadCancelThrow, Resource}

@derive(applyK)
trait PayloadsRepository[F[_]] {
  def create(payloadData: CreatePayload): F[Payload]

  def getByPaginated(
      did: DID,
      lastSeenIdOpt: Option[Payload.Id],
      limit: Int
  ): F[List[Payload]]
}

object PayloadsRepository {
  def create[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Functor](
      xa: Transactor[F],
      logs: Logs[R, F]
  ): R[PayloadsRepository[F]] =
    for {
      serviceLogs <- logs.service[PayloadsRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, PayloadsRepository[F]] = serviceLogs
      val metrics: PayloadsRepository[Mid[F, *]] = new PayloadsRepoMetrics
      val logging: PayloadsRepository[Mid[F, *]] = new PayloadsRepoLogging
      val mid = metrics |+| logging
      mid attach new PayloadsRepositoryImpl(xa)
    }

  def resource[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Applicative: Functor](
      xa: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, PayloadsRepository[F]] = Resource.eval(PayloadsRepository.create(xa, logs))

  def unsafe[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Comonad](
      xa: Transactor[F],
      logs: Logs[R, F]
  ): PayloadsRepository[F] = PayloadsRepository.create(xa, logs).extract
}

private class PayloadsRepositoryImpl[F[_]](xa: Transactor[F])(implicit
    br: MonadCancel[F, Throwable]
) extends PayloadsRepository[F] {
  override def create(payloadData: CreatePayload): F[Payload] =
    PayloadsDAO
      .createPayload(payloadData)
      .logSQLErrorsV2("creating")
      .transact(xa)

  def getByPaginated(
      did: DID,
      lastSeenIdOpt: Option[Payload.Id],
      limit: Int
  ): F[List[Payload]] =
    PayloadsDAO
      .getByPaginated(did, lastSeenIdOpt, limit)
      .logSQLErrorsV2("getting by paginated")
      .transact(xa)
}

private final class PayloadsRepoMetrics[F[_]: TimeMeasureMetric: MonadCancelThrow]
    extends PayloadsRepository[Mid[F, *]] {
  val repoName: String = "payloads-repository"

  private lazy val createTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "create")
  private lazy val getByPaginatedTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getByPaginated")

  override def create(payloadData: CreatePayload): Mid[F, Payload] =
    _.measureOperationTime(createTimer)

  override def getByPaginated(
      did: DID,
      lastSeenIdOpt: Option[Payload.Id],
      limit: Int
  ): Mid[F, List[Payload]] =
    _.measureOperationTime(getByPaginatedTimer)
}

private final class PayloadsRepoLogging[
    F[_]: MonadThrow: ServiceLogging[*[_], PayloadsRepository[F]]
] extends PayloadsRepository[Mid[F, *]] {
  override def create(payloadData: CreatePayload): Mid[F, Payload] =
    in =>
      info"creating payload ${payloadData.externalId}" *> in
        .flatTap(r => info"creating payload - successfully done ${r.id}")
        .onError(e => errorCause"an error occurred while creating payload" (e))

  override def getByPaginated(
      did: DID,
      lastSeenIdOpt: Option[Payload.Id],
      limit: Int
  ): Mid[F, List[Payload]] =
    in =>
      info"getting paginated data ${did.asCanonical().toString} {limit=$limit}" *> in
        .flatTap(r => info"getting paginated data - successfully done got ${r.size} entities")
        .onError(e => errorCause"an error occurred while creating payload" (e))

}
