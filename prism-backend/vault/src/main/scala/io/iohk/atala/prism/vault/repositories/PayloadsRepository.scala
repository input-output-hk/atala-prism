package io.iohk.atala.prism.vault.repositories

import cats.effect.Bracket
import doobie.Transactor
import doobie.implicits._
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.vault.model.{CreatePayload, Payload}
import io.iohk.atala.prism.vault.repositories.daos.PayloadsDAO
import org.slf4j.{Logger, LoggerFactory}
import derevo.derive
import derevo.tagless.applyK
import tofu.higherKind.Mid

@derive(applyK)
trait PayloadsRepository[F[_]] {
  def create(payloadData: CreatePayload): F[Payload]

  def getByPaginated(did: DID, lastSeenIdOpt: Option[Payload.Id], limit: Int): F[List[Payload]]
}

object PayloadsRepository {
  def apply[F[_]](
      xa: Transactor[F]
  )(implicit br: Bracket[F, Throwable], m: TimeMeasureMetric[F]): PayloadsRepository[F] = {
    val metrics: PayloadsRepository[Mid[F, *]] = new PayloadsRepoMetrics[F]()
    metrics attach new PayloadsRepositoryImpl(xa)
  }
}

private class PayloadsRepositoryImpl[F[_]](xa: Transactor[F])(implicit br: Bracket[F, Throwable])
    extends PayloadsRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def create(payloadData: CreatePayload): F[Payload] =
    PayloadsDAO
      .createPayload(payloadData)
      .logSQLErrors("creating", logger)
      .transact(xa)

  def getByPaginated(
      did: DID,
      lastSeenIdOpt: Option[Payload.Id],
      limit: Int
  ): F[List[Payload]] =
    PayloadsDAO
      .getByPaginated(did, lastSeenIdOpt, limit)
      .logSQLErrors("getting by paginated", logger)
      .transact(xa)
}

private final class PayloadsRepoMetrics[F[_]: TimeMeasureMetric](implicit br: Bracket[F, Throwable])
    extends PayloadsRepository[Mid[F, *]] {
  val repoName: String = "payloads-repository"

  private lazy val createTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "create")
  private lazy val getByPaginatedTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "getByPaginated")

  override def create(payloadData: CreatePayload): Mid[F, Payload] = _.measureOperationTime(createTimer)

  override def getByPaginated(did: DID, lastSeenIdOpt: Option[Payload.Id], limit: Int): Mid[F, List[Payload]] =
    _.measureOperationTime(getByPaginatedTimer)
}
