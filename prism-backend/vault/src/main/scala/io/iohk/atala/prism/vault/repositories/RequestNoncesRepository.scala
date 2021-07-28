package io.iohk.atala.prism.vault.repositories

import cats.effect.BracketThrow
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.vault.repositories.daos.RequestNoncesDAO
import io.iohk.atala.prism.logging.GeneralLoggableInstances._
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.Util._
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid
import tofu.logging.Logging

@derive(applyK)
trait RequestNoncesRepository[F[_]] {
  def burn(did: DID, requestNonce: RequestNonce): F[Unit]
}

object RequestNoncesRepository {
  object PostgresImpl {
    def create[F[_]: BracketThrow: TimeMeasureMetric: Logging](xa: Transactor[F]): RequestNoncesRepository[F] = {
      val metrics: RequestNoncesRepository[Mid[F, *]] =
        new RequestNoncesRepositoryMetrics[F]("RequestNoncesRepositoryPostgresImpl")
      metrics attach new PostgresImpl(xa)
    }
  }
}

private class PostgresImpl[F[_]: BracketThrow](xa: Transactor[F]) extends RequestNoncesRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def burn(did: DID, requestNonce: RequestNonce): F[Unit] =
    RequestNoncesDAO
      .burn(did, requestNonce)
      .logSQLErrors("burning", logger)
      .transact(xa)
}

private final class RequestNoncesRepositoryMetrics[F[_]: TimeMeasureMetric: BracketThrow](repoName: String)
    extends RequestNoncesRepository[Mid[F, *]] {
  private lazy val burnTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "burn")
  override def burn(did: DID, requestNonce: RequestNonce): Mid[F, Unit] = _.measureOperationTime(burnTimer)
}

private final class RequestNoncesRepositoryLogging[F[_]: Logging: BracketThrow]
    extends RequestNoncesRepository[Mid[F, *]] {
  override def burn(did: DID, requestNonce: RequestNonce): Mid[F, Unit] =
    _.logInfoAroundUnit("burning", did, TraceId.generateYOLO)
}
