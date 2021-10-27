package io.iohk.atala.prism.management.console.repositories

import cats.{Comonad, Functor, Monad}
import cats.effect.{BracketThrow, Resource}
import cats.syntax.comonad._
import cats.syntax.functor._
import derevo.tagless.applyK
import derevo.derive
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.daos.ReceivedCredentialsDAO
import io.iohk.atala.prism.management.console.repositories.daos.ReceivedCredentialsDAO.ReceivedSignedCredentialData
import io.iohk.atala.prism.management.console.repositories.logs.ReceivedCredentialsRepositoryLogs
import io.iohk.atala.prism.management.console.repositories.metrics.ReceivedCredentialsRepositoryMetrics
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps

@derive(applyK)
trait ReceivedCredentialsRepository[F[_]] {

  def getCredentialsFor(
      verifierId: ParticipantId,
      contactId: Option[Contact.Id]
  ): F[List[ReceivedSignedCredential]]

  def createReceivedCredential(data: ReceivedSignedCredentialData): F[Unit]

  def getLatestCredentialExternalId(
      verifierId: ParticipantId
  ): F[Option[CredentialExternalId]]

}

object ReceivedCredentialsRepository {

  def apply[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[ReceivedCredentialsRepository[F]] =
    for {
      serviceLogs <- logs.service[ReceivedCredentialsRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, ReceivedCredentialsRepository[F]] = serviceLogs
      val metrics: ReceivedCredentialsRepository[Mid[F, *]] =
        new ReceivedCredentialsRepositoryMetrics[F]
      val logs: ReceivedCredentialsRepository[Mid[F, *]] =
        new ReceivedCredentialsRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new ReceivedCredentialsRepositoryImpl[F](transactor)
    }

  def unsafe[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): ReceivedCredentialsRepository[F] =
    ReceivedCredentialsRepository(transactor, logs).extract

  def makeResource[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Monad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, ReceivedCredentialsRepository[F]] =
    Resource.eval(ReceivedCredentialsRepository(transactor, logs))

}

private final class ReceivedCredentialsRepositoryImpl[F[_]: BracketThrow](
    xa: Transactor[F]
) extends ReceivedCredentialsRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def getCredentialsFor(
      verifierId: ParticipantId,
      contactId: Option[Contact.Id]
  ): F[List[ReceivedSignedCredential]] =
    ReceivedCredentialsDAO
      .getReceivedCredentialsFor(verifierId, contactId)
      .logSQLErrors(s"getting credentials, verifier id - $verifierId", logger)
      .transact(xa)

  def createReceivedCredential(data: ReceivedSignedCredentialData): F[Unit] =
    ReceivedCredentialsDAO
      .insertSignedCredential(data)
      .logSQLErrors("creating received credentials", logger)
      .transact(xa)

  def getLatestCredentialExternalId(
      verifierId: ParticipantId
  ): F[Option[CredentialExternalId]] =
    ReceivedCredentialsDAO
      .getLatestCredentialExternalId(verifierId)
      .logSQLErrors(
        s"getting latest credential external id, verifier id -  $verifierId",
        logger
      )
      .transact(xa)
}
