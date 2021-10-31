package io.iohk.atala.prism.connector.repositories

import cats.{Applicative, Comonad, Functor}
import cats.effect.{BracketThrow, Resource}
import cats.syntax.comonad._
import cats.syntax.functor._
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.connector.repositories.daos.RequestNoncesDAO
import io.iohk.atala.prism.connector.repositories.logs.RequestNoncesRepositoryLogs
import io.iohk.atala.prism.connector.repositories.metrics.RequestNoncesRepositoryMetrics
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps

@derive(applyK)
trait RequestNoncesRepository[F[_]] {
  def burn(participantId: ParticipantId, requestNonce: RequestNonce): F[Unit]
  def burn(did: DID, requestNonce: RequestNonce): F[Unit]
}

object RequestNoncesRepository {
  def apply[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[RequestNoncesRepository[F]] =
    for {
      serviceLogs <- logs.service[RequestNoncesRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, RequestNoncesRepository[F]] =
        serviceLogs
      val metrics: RequestNoncesRepository[Mid[F, *]] =
        new RequestNoncesRepositoryMetrics[F]
      val logs: RequestNoncesRepository[Mid[F, *]] =
        new RequestNoncesRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new RequestNoncesRepositoryPostgresImpl[F](transactor)
    }

  def resource[F[_]: TimeMeasureMetric: BracketThrow, R[
      _
  ]: Applicative: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, RequestNoncesRepository[F]] =
    Resource.eval(RequestNoncesRepository(transactor, logs))

  def unsafe[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): RequestNoncesRepository[F] =
    RequestNoncesRepository(transactor, logs).extract
}

private final class RequestNoncesRepositoryPostgresImpl[F[_]: BracketThrow](
    xa: Transactor[F]
) extends RequestNoncesRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def burn(
      participantId: ParticipantId,
      requestNonce: RequestNonce
  ): F[Unit] = {
    RequestNoncesDAO
      .burn(participantId, requestNonce)
      .logSQLErrors(s"burning, participant id - $participantId", logger)
      .transact(xa)
  }

  override def burn(did: DID, requestNonce: RequestNonce): F[Unit] = {
    RequestNoncesDAO
      .burn(did, requestNonce)
      .logSQLErrors(s"burning, did - $did", logger)
      .transact(xa)
  }
}
