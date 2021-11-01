package io.iohk.atala.prism.vault.repositories

import cats.{Applicative, Comonad, Functor}
import cats.effect.{BracketThrow, Resource}
import cats.syntax.apply._
import cats.syntax.comonad._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.vault.repositories.daos.RequestNoncesDAO
import io.iohk.atala.prism.logging.GeneralLoggableInstances._
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps
import tofu.syntax.logging._

@derive(applyK)
trait RequestNoncesRepository[F[_]] {
  def burn(did: DID, requestNonce: RequestNonce): F[Unit]
}

object RequestNoncesRepository {
  object PostgresImpl {
    def create[F[_]: BracketThrow: TimeMeasureMetric, R[_]: Functor](
        xa: Transactor[F],
        logs: Logs[R, F]
    ): R[RequestNoncesRepository[F]] =
      for {
        serviceLogs <- logs.service[RequestNoncesRepository[F]]
      } yield {
        implicit val implicitLogs: ServiceLogging[F, RequestNoncesRepository[F]] = serviceLogs
        val metrics: RequestNoncesRepository[Mid[F, *]] = new RequestNoncesRepositoryMetrics
        val logging: RequestNoncesRepository[Mid[F, *]] = new RequestNoncesRepositoryLogging
        val mid = metrics |+| logging
        mid attach new PostgresImpl(xa)
      }

    def resource[F[_]: BracketThrow: TimeMeasureMetric, R[_]: Applicative: Functor](
        xa: Transactor[F],
        logs: Logs[R, F]
    ): Resource[R, RequestNoncesRepository[F]] = Resource.eval(RequestNoncesRepository.PostgresImpl.create(xa, logs))

    def unsafe[F[_]: BracketThrow: TimeMeasureMetric, R[_]: Comonad](
        xa: Transactor[F],
        logs: Logs[R, F]
    ): RequestNoncesRepository[F] = RequestNoncesRepository.PostgresImpl.create(xa, logs).extract
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

private final class RequestNoncesRepositoryMetrics[F[
    _
]: TimeMeasureMetric: BracketThrow]
    extends RequestNoncesRepository[Mid[F, *]] {
  val repoName = "RequestNoncesRepositoryPostgresImpl"
  private lazy val burnTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "burn")
  override def burn(did: DID, requestNonce: RequestNonce): Mid[F, Unit] =
    _.measureOperationTime(burnTimer)
}

private final class RequestNoncesRepositoryLogging[
    F[_]: BracketThrow: ServiceLogging[*[_], RequestNoncesRepository[F]]
] extends RequestNoncesRepository[Mid[F, *]] {
  override def burn(did: DID, requestNonce: RequestNonce): Mid[F, Unit] =
    in =>
      info"burning nonce $did" *> in
        .flatTap(_ => info"burning nonce - successfully done")
        .onError(e => errorCause"an error occurred while burning nonce" (e))

}
