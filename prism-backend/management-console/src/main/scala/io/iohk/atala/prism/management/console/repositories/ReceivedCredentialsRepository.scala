package io.iohk.atala.prism.management.console.repositories

import cats.effect.BracketThrow
import derevo.tagless.applyK
import derevo.derive
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CredentialExternalId,
  ParticipantId,
  ReceivedSignedCredential
}
import io.iohk.atala.prism.management.console.repositories.daos.ReceivedCredentialsDAO
import io.iohk.atala.prism.management.console.repositories.daos.ReceivedCredentialsDAO.ReceivedSignedCredentialData
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid

@derive(applyK)
trait ReceivedCredentialsRepository[F[_]] {

  def getCredentialsFor(
      verifierId: ParticipantId,
      contactId: Option[Contact.Id]
  ): F[List[ReceivedSignedCredential]]

  def createReceivedCredential(data: ReceivedSignedCredentialData): F[Unit]

  def getLatestCredentialExternalId(verifierId: ParticipantId): F[Option[CredentialExternalId]]

}

object ReceivedCredentialsRepository {

  def apply[F[_]: TimeMeasureMetric: BracketThrow](transactor: Transactor[F]): ReceivedCredentialsRepository[F] = {
    val metrics: ReceivedCredentialsRepository[Mid[F, *]] = new ReceivedCredentialsRepositoryMetrics[F]
    metrics attach new ReceivedCredentialsRepositoryImpl[F](transactor)
  }

}

private final class ReceivedCredentialsRepositoryImpl[F[_]: BracketThrow](xa: Transactor[F])
    extends ReceivedCredentialsRepository[F] {

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

  def getLatestCredentialExternalId(verifierId: ParticipantId): F[Option[CredentialExternalId]] =
    ReceivedCredentialsDAO
      .getLatestCredentialExternalId(verifierId)
      .logSQLErrors(s"getting latest credential external id, verifier id -  $verifierId", logger)
      .transact(xa)
}

private final class ReceivedCredentialsRepositoryMetrics[F[_]: TimeMeasureMetric: BracketThrow]
    extends ReceivedCredentialsRepository[Mid[F, *]] {

  private val repoName = "ReceivedCredentialsRepository"
  private lazy val getCredentialsForTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "getCredentialsFor")
  private lazy val createReceivedCredentialTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "createReceivedCredential")
  private lazy val getLatestCredentialExternalIdTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getLatestCredentialExternalId")

  override def getCredentialsFor(
      verifierId: ParticipantId,
      contactId: Option[Contact.Id]
  ): Mid[F, List[ReceivedSignedCredential]] = _.measureOperationTime(getCredentialsForTimer)

  override def createReceivedCredential(data: ReceivedSignedCredentialData): Mid[F, Unit] =
    _.measureOperationTime(createReceivedCredentialTimer)

  override def getLatestCredentialExternalId(verifierId: ParticipantId): Mid[F, Option[CredentialExternalId]] =
    _.measureOperationTime(getLatestCredentialExternalIdTimer)
}
