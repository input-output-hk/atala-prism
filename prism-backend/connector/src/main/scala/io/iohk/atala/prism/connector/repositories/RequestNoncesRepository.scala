package io.iohk.atala.prism.connector.repositories

import cats.effect.{Bracket, BracketThrow}
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.connector.repositories.daos.RequestNoncesDAO
import io.iohk.atala.prism.connector.repositories.metrics.RequestNoncesRepositoryMetrics
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import org.slf4j.{Logger, LoggerFactory}

@derive(applyK)
trait RequestNoncesRepository[F[_]] {
  def burn(participantId: ParticipantId, requestNonce: RequestNonce): F[Unit]
  def burn(did: DID, requestNonce: RequestNonce): F[Unit]
}

object RequestNoncesRepository {
  def apply[F[_]: TimeMeasureMetric](
      transactor: Transactor[F]
  )(implicit br: Bracket[F, Throwable]): RequestNoncesRepository[F] = {
    val metrics = new RequestNoncesRepositoryMetrics[F]
    metrics attach new RequestNoncesRepositoryPostgresImpl[F](transactor)
  }
}

private final class RequestNoncesRepositoryPostgresImpl[F[_]: BracketThrow](xa: Transactor[F])
    extends RequestNoncesRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def burn(participantId: ParticipantId, requestNonce: RequestNonce): F[Unit] = {
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
