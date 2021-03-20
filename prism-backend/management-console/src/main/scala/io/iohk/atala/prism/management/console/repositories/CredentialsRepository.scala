package io.iohk.atala.prism.management.console.repositories

import cats.data.{EitherT, NonEmptyList}
import cats.data.Validated.{Invalid, Valid}
import cats.effect.IO
import doobie.ConnectionIO
import doobie.util.transactor.Transactor
import doobie.implicits._
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.management.console.errors.{
  ContactIdsWereNotFound,
  CredentialDataValidationFailed,
  ExternalIdsWereNotFound,
  ManagementConsoleError,
  MissingContactIdAndExternalId,
  PublishedCredentialsNotExist
}
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CreateGenericCredential,
  CredentialTypeWithRequiredFields,
  GenericCredential,
  ParticipantId,
  PublishCredential
}
import io.iohk.atala.prism.management.console.repositories.daos.{ContactsDAO, CredentialTypeDao, CredentialsDAO}
import io.iohk.atala.prism.management.console.validations.CredentialDataValidator
import io.iohk.atala.prism.models.TransactionInfo
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.{FutureEitherFOps, FutureEitherOps}

import scala.concurrent.ExecutionContext

class CredentialsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  def create(
      participantId: ParticipantId,
      data: CreateGenericCredential
  ): FutureEither[ManagementConsoleError, GenericCredential] = {

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
        //validate credential data
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

    transaction
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }

  def getBy(credentialId: GenericCredential.Id): FutureEither[Nothing, Option[GenericCredential]] = {
    CredentialsDAO
      .getBy(credentialId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getBy(
      issuedBy: ParticipantId,
      limit: Int,
      lastSeenCredential: Option[GenericCredential.Id]
  ): FutureEither[Nothing, List[GenericCredential]] = {
    CredentialsDAO
      .getBy(issuedBy, limit, lastSeenCredential)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getBy(issuedBy: ParticipantId, subjectId: Contact.Id): FutureEither[Nothing, List[GenericCredential]] = {
    CredentialsDAO
      .getBy(issuedBy, subjectId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def storePublicationData(issuerId: ParticipantId, credentialData: PublishCredential): FutureEither[Nothing, Int] = {
    CredentialsDAO
      .storePublicationData(issuerId, credentialData)
      .transact(xa)
      .unsafeToFuture()
      .lift
  }

  def markAsShared(
      issuerId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): FutureEither[Nothing, Unit] = {
    CredentialsDAO
      .markAsShared(issuerId, credentialsIds)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def verifyPublishedCredentialsExist(
      issuerId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): FutureEither[ManagementConsoleError, Unit] = {
    CredentialsDAO
      .verifyPublishedCredentialsExist(issuerId, credentialsIds)
      .transact(xa)
      .unsafeToFuture()
      .map { existingCredentialIds =>
        val nonExistingCredentials = credentialsIds.toList.toSet.diff(existingCredentialIds.toSet)
        if (nonExistingCredentials.nonEmpty) Left(PublishedCredentialsNotExist(nonExistingCredentials.toList))
        else Right(())
      }
      .toFutureEither
  }

  def storeBatchData(
      batchId: CredentialBatchId,
      issuanceOperationHash: SHA256Digest,
      issuanceTransactionInfo: TransactionInfo
  ): FutureEither[Nothing, Int] = {
    CredentialsDAO
      .storeBatchData(batchId, issuanceTransactionInfo, issuanceOperationHash)
      .transact(xa)
      .unsafeToFuture()
      .lift
  }
}
