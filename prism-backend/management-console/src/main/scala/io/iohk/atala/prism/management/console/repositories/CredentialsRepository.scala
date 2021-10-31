package io.iohk.atala.prism.management.console.repositories

import cats.{Comonad, Functor, Monad}
import cats.syntax.comonad._
import cats.syntax.functor._
import cats.data.{EitherT, NonEmptyList}
import cats.data.Validated.{Invalid, Valid}
import cats.effect.{BracketThrow, Resource}
import derevo.tagless.applyK
import derevo.derive
import doobie.{ConnectionIO, FC}
import doobie.util.transactor.Transactor
import doobie.implicits._
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.management.console.errors._
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.daos.{ContactsDAO, CredentialTypeDao, CredentialsDAO}
import io.iohk.atala.prism.management.console.repositories.logs.CredentialsRepositoryLogs
import io.iohk.atala.prism.management.console.repositories.metrics.CredentialsRepositoryMetrics
import io.iohk.atala.prism.management.console.validations.CredentialDataValidator
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps

@derive(applyK)
trait CredentialsRepository[F[_]] {

  def create(
      participantId: ParticipantId,
      data: CreateGenericCredential
  ): F[Either[ManagementConsoleError, GenericCredential]]

  def getBy(credentialId: GenericCredential.Id): F[Option[GenericCredential]]

  def getBy(
      issuedBy: ParticipantId,
      query: GenericCredential.PaginatedQuery
  ): F[List[GenericCredential]]

  def getBy(
      issuedBy: ParticipantId,
      limit: Int,
      lastSeenCredential: Option[GenericCredential.Id]
  ): F[List[GenericCredential]]

  def getBy(issuedBy: ParticipantId, contactId: Contact.Id): F[List[GenericCredential]]

  def storePublicationData(issuerId: ParticipantId, credentialData: PublishCredential): F[Int]

  def markAsShared(
      issuerId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): F[Unit]

  def verifyPublishedCredentialsExist(
      issuerId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): F[Either[ManagementConsoleError, Unit]]

  def storeBatchData(
      batchId: CredentialBatchId,
      issuanceOperationHash: Sha256Digest,
      atalaOperationId: AtalaOperationId
  ): F[Int]

  def deleteCredentials(
      institutionId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): F[Either[ManagementConsoleError, Unit]]

  def storeRevocationData(
      institutionId: ParticipantId,
      credentialId: GenericCredential.Id,
      operationId: AtalaOperationId
  ): F[Unit]

}

object CredentialsRepository {

  def apply[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[CredentialsRepository[F]] =
    for {
      serviceLogs <- logs.service[CredentialsRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, CredentialsRepository[F]] = serviceLogs
      val metrics: CredentialsRepository[Mid[F, *]] = new CredentialsRepositoryMetrics[F]
      val logs: CredentialsRepository[Mid[F, *]] = new CredentialsRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new CredentialsRepositoryImpl[F](transactor)
    }

  def unsafe[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): CredentialsRepository[F] = CredentialsRepository(transactor, logs).extract

  def makeResource[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Monad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, CredentialsRepository[F]] = Resource.eval(CredentialsRepository(transactor, logs))

}

private final class CredentialsRepositoryImpl[F[_]: BracketThrow](xa: Transactor[F]) extends CredentialsRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def create(
      participantId: ParticipantId,
      data: CreateGenericCredential
  ): F[Either[ManagementConsoleError, GenericCredential]] = {

    def validateCredentialData(
        credentialTypeWithRequiredFields: CredentialTypeWithRequiredFields
    ): EitherT[ConnectionIO, ManagementConsoleError, Unit] = {
      CredentialDataValidator.validate(credentialTypeWithRequiredFields, data.credentialData) match {
        case Valid(_) => EitherT.fromEither[ConnectionIO](Right(()))
        case Invalid(errors) =>
          EitherT.fromEither[ConnectionIO](
            Left(
              CredentialDataValidationFailed(
                credentialTypeWithRequiredFields.credentialType.name,
                data.credentialData,
                errors.toList
              )
            )
          )
      }
    }

    // get contactId from the externalId
    // TODO: Avoid doing this when we stop accepting the contactId
    val contactF = data.externalId match {
      case Some(externalId) =>
        EitherT.fromOptionF(
          ContactsDAO.findContact(participantId, externalId),
          ExternalIdsWereNotFound(Set(externalId)): ManagementConsoleError
        )
      case None =>
        data.contactId match {
          case Some(contactId) =>
            EitherT.fromOptionF(
              ContactsDAO.findContact(participantId, contactId),
              ContactIdsWereNotFound(Set(contactId)): ManagementConsoleError
            )
          case None =>
            EitherT.leftT[ConnectionIO, Contact](
              MissingContactIdAndExternalId: ManagementConsoleError
            )
        }
    }

    val transaction =
      for {
        // validate credential data
        credentialTypeWithRequiredFields <-
          EitherT[ConnectionIO, ManagementConsoleError, CredentialTypeWithRequiredFields](
            CredentialTypeDao.findValidated(data.credentialTypeId, participantId)
          )
        _ <- validateCredentialData(credentialTypeWithRequiredFields)
        contact <- contactF
        credential <- EitherT.right[ManagementConsoleError](
          CredentialsDAO.create(participantId, contact.contactId, data)
        )
      } yield credential

    transaction.value
      .logSQLErrors(s"creating credential, participant id - $participantId", logger)
      .transact(xa)
  }

  def getBy(credentialId: GenericCredential.Id): F[Option[GenericCredential]] =
    CredentialsDAO
      .getBy(credentialId)
      .logSQLErrors(s"getting, credential id - $credentialId", logger)
      .transact(xa)

  def getBy(
      issuedBy: ParticipantId,
      query: GenericCredential.PaginatedQuery
  ): F[List[GenericCredential]] =
    CredentialsDAO
      .getBy(issuedBy, query)
      .logSQLErrors(s"getting, issued id - $issuedBy", logger)
      .transact(xa)

  def getBy(
      issuedBy: ParticipantId,
      limit: Int,
      lastSeenCredential: Option[GenericCredential.Id]
  ): F[List[GenericCredential]] =
    CredentialsDAO
      .getBy(issuedBy, limit, lastSeenCredential)
      .logSQLErrors(s"getting, issued id - $issuedBy", logger)
      .transact(xa)

  def getBy(issuedBy: ParticipantId, contactId: Contact.Id): F[List[GenericCredential]] =
    CredentialsDAO
      .getBy(issuedBy, contactId)
      .logSQLErrors(s"getting, contact id - $contactId", logger)
      .transact(xa)

  def storePublicationData(issuerId: ParticipantId, credentialData: PublishCredential): F[Int] =
    CredentialsDAO
      .storePublicationData(issuerId, credentialData)
      .logSQLErrors(s"store publication data, issuer id - $issuerId", logger)
      .transact(xa)

  def markAsShared(
      issuerId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): F[Unit] =
    CredentialsDAO
      .markAsShared(issuerId, credentialsIds)
      .logSQLErrors(s"marking as shared, issuer id - $issuerId", logger)
      .transact(xa)

  def verifyPublishedCredentialsExist(
      issuerId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): F[Either[ManagementConsoleError, Unit]] =
    CredentialsDAO
      .verifyPublishedCredentialsExist(issuerId, credentialsIds)
      .logSQLErrors(s"verifying published credentials exists, issuer id - $issuerId", logger)
      .transact(xa)
      .map { existingCredentialIds =>
        val nonExistingCredentials = credentialsIds.toList.toSet.diff(existingCredentialIds.toSet)
        if (nonExistingCredentials.nonEmpty) Left(PublishedCredentialsNotExist(nonExistingCredentials.toList))
        else Right(())
      }

  def storeBatchData(
      batchId: CredentialBatchId,
      issuanceOperationHash: Sha256Digest,
      atalaOperationId: AtalaOperationId
  ): F[Int] =
    CredentialsDAO
      .storeBatchData(batchId, issuanceOperationHash, atalaOperationId)
      .logSQLErrors(s"storing batch data, batch id - $batchId", logger)
      .transact(xa)

  def deleteCredentials(
      institutionId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): F[Either[ManagementConsoleError, Unit]] = {
    val deleteIO: ConnectionIO[Either[ManagementConsoleError, Unit]] = for {
      _ <- CredentialsDAO.deletePublishedCredentialsBy(institutionId, credentialsIds)
      _ <- CredentialsDAO.deleteBy(institutionId, credentialsIds)
    } yield Right(())

    CredentialsDAO
      .getIdsOfPublishedNotRevokedCredentials(institutionId, credentialsIds)
      .flatMap { publishNotRevokedCredentials =>
        if (publishNotRevokedCredentials.nonEmpty)
          FC.pure[Either[ManagementConsoleError, Unit]](
            Left(PublishedCredentialsNotRevoked(publishNotRevokedCredentials))
          )
        else
          deleteIO
      }
      .logSQLErrors(s"deleting credentials, institutionId = $institutionId", logger)
      .transact(xa)
  }

  def storeRevocationData(
      institutionId: ParticipantId,
      credentialId: GenericCredential.Id,
      operationId: AtalaOperationId
  ): F[Unit] =
    CredentialsDAO
      .revokeCredential(institutionId, credentialId, operationId)
      .logSQLErrors(s"storing revocation data, institution id - $institutionId", logger)
      .transact(xa)
}
