package io.iohk.atala.prism.management.console.repositories

import cats.effect.BracketThrow
import derevo.tagless.applyK
import derevo.derive
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.management.console.models.ParticipantId
import io.iohk.atala.prism.management.console.repositories.daos.RequestNoncesDAO
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid

@derive(applyK)
trait RequestNoncesRepository[F[_]] {
  def burn(participantId: ParticipantId, requestNonce: RequestNonce): F[Unit]
}

object RequestNoncesRepository {

  def apply[F[_]: TimeMeasureMetric: BracketThrow](transactor: Transactor[F]): RequestNoncesRepository[F] = {
    val metrics: RequestNoncesRepository[Mid[F, *]] = new RequestNoncesRepositoryMetrics[F]
    metrics attach new RequestNoncesRepositoryPostgresImpl[F](transactor)
  }

}

private final class RequestNoncesRepositoryPostgresImpl[F[_]: BracketThrow](xa: Transactor[F])
    extends RequestNoncesRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def burn(participantId: ParticipantId, requestNonce: RequestNonce): F[Unit] =
    RequestNoncesDAO
      .burn(participantId, requestNonce)
      .logSQLErrors(s"burning, participant id - $participantId", logger)
      .transact(xa)
}

private final class RequestNoncesRepositoryMetrics[F[_]: TimeMeasureMetric: BracketThrow]
    extends RequestNoncesRepository[Mid[F, *]] {
  private val repoName = "RequestNoncesRepositoryPostgresImpl"
  private lazy val burnTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "burn")
  override def burn(participantId: ParticipantId, requestNonce: RequestNonce): Mid[F, Unit] =
    _.measureOperationTime(burnTimer)
}
