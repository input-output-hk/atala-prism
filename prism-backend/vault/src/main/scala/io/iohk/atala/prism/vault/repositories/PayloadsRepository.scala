package io.iohk.atala.prism.vault.repositories

import cats.effect.{Bracket, BracketThrow, MonadThrow}
import doobie.Transactor
import doobie.implicits._
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.vault.model.{CreatePayload, Payload}
import io.iohk.atala.prism.vault.repositories.daos.PayloadsDAO
import io.iohk.atala.prism.logging.GeneralLoggableInstances._
import io.iohk.atala.prism.logging.Util._
import org.slf4j.{Logger, LoggerFactory}
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.logging.TraceId
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.monoid.TofuSemigroupOps

@derive(applyK)
trait PayloadsRepository[F[_]] {
  def create(payloadData: CreatePayload, tId: TraceId): F[Payload]

  def getByPaginated(did: DID, lastSeenIdOpt: Option[Payload.Id], limit: Int, tId: TraceId): F[List[Payload]]
}

object PayloadsRepository {
  def create[F[_]](xa: Transactor[F])(implicit
      br: Bracket[F, Throwable],
      m: TimeMeasureMetric[F],
      logs: ServiceLogging[F, PayloadsRepository[F]]
  ): PayloadsRepository[F] = {
    val mid = (new PayloadsRepoMetrics: PayloadsRepository[Mid[F, *]]) |+| (new PayloadsRepoLogging: PayloadsRepository[
      Mid[F, *]
    ])
    mid attach new PayloadsRepositoryImpl(xa)
  }
}

private class PayloadsRepositoryImpl[F[_]](xa: Transactor[F])(implicit br: Bracket[F, Throwable])
    extends PayloadsRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def create(payloadData: CreatePayload, tId: TraceId): F[Payload] =
    PayloadsDAO
      .createPayload(payloadData)
      .logSQLErrors("creating", logger)
      .transact(xa)

  def getByPaginated(
      did: DID,
      lastSeenIdOpt: Option[Payload.Id],
      limit: Int,
      tId: TraceId
  ): F[List[Payload]] =
    PayloadsDAO
      .getByPaginated(did, lastSeenIdOpt, limit)
      .logSQLErrors("getting by paginated", logger)
      .transact(xa)
}

private final class PayloadsRepoMetrics[F[_]: TimeMeasureMetric: BracketThrow] extends PayloadsRepository[Mid[F, *]] {
  val repoName: String = "payloads-repository"

  private lazy val createTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "create")
  private lazy val getByPaginatedTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "getByPaginated")

  override def create(payloadData: CreatePayload, tId: TraceId): Mid[F, Payload] = _.measureOperationTime(createTimer)

  override def getByPaginated(
      did: DID,
      lastSeenIdOpt: Option[Payload.Id],
      limit: Int,
      tId: TraceId
  ): Mid[F, List[Payload]] =
    _.measureOperationTime(getByPaginatedTimer)
}

private final class PayloadsRepoLogging[F[_]: MonadThrow](implicit logs: ServiceLogging[F, PayloadsRepository[F]])
    extends PayloadsRepository[Mid[F, *]] {
  override def create(payloadData: CreatePayload, tId: TraceId): Mid[F, Payload] =
    _.logInfoAround[Payload.ExternalId, Payload.Id, PayloadsRepository[F]](
      "creating payload",
      payloadData.externalId,
      tId
    )

  override def getByPaginated(
      did: DID,
      lastSeenIdOpt: Option[Payload.Id],
      limit: Int,
      tId: TraceId
  ): Mid[F, List[Payload]] = _.logInfoAroundList("getting data by paginated", did, tId)
}
