package io.iohk.atala.prism.management.console.repositories

import cats.data.EitherT
import cats.effect.IO
import doobie.ConnectionIO
import doobie.util.transactor.Transactor
import doobie.implicits._
import io.iohk.atala.prism.management.console.errors.{
  ContactIdsWereNotFound,
  ExternalIdsWereNotFound,
  ManagementConsoleError,
  MissingContactIdAndExternalId
}
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CreateGenericCredential,
  GenericCredential,
  ParticipantId,
  PublishCredential
}
import io.iohk.atala.prism.management.console.repositories.daos.{ContactsDAO, CredentialsDAO}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class CredentialsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  def create(
      participantId: ParticipantId,
      data: CreateGenericCredential
  ): FutureEither[ManagementConsoleError, GenericCredential] = {

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
      .map(Right(_))
      .toFutureEither
  }

  def markAsShared(issuerId: ParticipantId, credentialId: GenericCredential.Id): FutureEither[Nothing, Unit] = {
    CredentialsDAO
      .markAsShared(issuerId, credentialId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
