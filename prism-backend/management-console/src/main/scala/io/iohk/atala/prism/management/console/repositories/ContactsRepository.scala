package io.iohk.atala.prism.management.console.repositories

import cats.data.EitherT
import cats.effect._
import cats.implicits._
import doobie.free._
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CreateContact,
  InstitutionGroup,
  ParticipantId,
  UpdateContact
}
import io.iohk.atala.prism.management.console.repositories.daos.{ContactsDAO, CredentialsDAO, InstitutionGroupsDAO}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import java.time.Instant
import scala.concurrent.ExecutionContext

class ContactsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(
      contactData: CreateContact,
      maybeGroupName: Option[InstitutionGroup.Name],
      createdAt: Instant = Instant.now()
  ): FutureEither[ManagementConsoleError, Contact] = {
    val query = maybeGroupName match {
      case None => // if we do not request the contact to be added to a group
        ContactsDAO.createContact(contactData, createdAt)
      case Some(groupName) => // if we are requesting to add a contact to a group
        for {
          contact <- ContactsDAO.createContact(contactData, createdAt)
          groupMaybe <- InstitutionGroupsDAO.find(contactData.createdBy, groupName)
          group = groupMaybe.getOrElse(throw new RuntimeException(s"Group $groupName does not exist"))
          _ <- InstitutionGroupsDAO.addContact(group.id, contact.contactId)
        } yield contact
    }

    query
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def createBatch(
      institutionId: ParticipantId,
      request: CreateContact.Batch
  ): FutureEither[ManagementConsoleError, Unit] = {
    def unsafe = {
      for {
        contactIds <- ContactsDAO.createContacts(institutionId, request.contacts, Instant.now())
        _ <- InstitutionGroupsDAO.addContacts(request.groups, contactIds.toSet)
      } yield ().asRight[ManagementConsoleError]
    }

    val connectionIO = for {
      errorMaybe <- institutionHelper.checkGroups(institutionId, request.groups)
      result <- errorMaybe match {
        case Some(consoleError) => connection.pure(consoleError.asLeft[Unit])
        case None => unsafe
      }
    } yield result

    connectionIO
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither
  }

  def updateContact(institutionId: ParticipantId, request: UpdateContact): FutureEither[Nothing, Unit] = {
    ContactsDAO
      .updateContact(institutionId, request)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }

  def find(institutionId: ParticipantId, contactId: Contact.Id): FutureEither[Nothing, Option[Contact]] = {
    ContactsDAO
      .findContact(institutionId, contactId)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }

  def find(institutionId: ParticipantId, externalId: Contact.ExternalId): FutureEither[Nothing, Option[Contact]] = {
    ContactsDAO
      .findContact(institutionId, externalId)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }

  def findContacts(institutionId: ParticipantId, contactIds: List[Contact.Id]): FutureEither[Nothing, List[Contact]] = {
    ContactsDAO
      .findContacts(institutionId, contactIds)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }

  def getBy(
      createdBy: ParticipantId,
      constraints: Contact.PaginatedQuery
  ): FutureEither[ManagementConsoleError, Seq[Contact.WithCredentialCounts]] = {
    ContactsDAO
      .getBy(createdBy, constraints)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def delete(
      institutionId: ParticipantId,
      contactId: Contact.Id,
      deleteCredentials: Boolean
  ): FutureEither[ManagementConsoleError, Unit] = {
    def performDeletion(): ConnectionIO[Either[ManagementConsoleError, Unit]] =
      for {
        _ <-
          if (deleteCredentials) {
            // First we delete published credentials and then we delete all the draft credentials
            // The order is important as published credentials depend on draft ones
            CredentialsDAO.deletePublishedCredentialsBy(contactId) *>
              CredentialsDAO.deleteBy(contactId)
          } else {
            doobie.free.connection.unit
          }
        _ <- InstitutionGroupsDAO.removeContact(contactId)
        isDeleted <- ContactsDAO.delete(institutionId, contactId)
        // If the contact was not deleted after all the checks, something is wrong
        _ = if (!isDeleted) throw new IllegalStateException("The requested contact was not deleted")
      } yield Right(())

    val connectionIO = for {
      // Check that the contact belongs to the correct institution
      _ <- EitherT(institutionHelper.checkContacts(institutionId, Set(contactId)).map(_.toLeft(())))
      _ <-
        if (deleteCredentials) {
          EitherT.rightT[ConnectionIO, ManagementConsoleError][Unit](())
        } else {
          // Check if we are trying to delete a contact without deleting its existing credentials
          EitherT(institutionHelper.checkCredentialsAreEmpty(institutionId, contactId).map(_.toLeft(())))
        }
      // All checks have passed, we can actually start deleting now
      result <- EitherT(performDeletion())
    } yield result

    connectionIO.value
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither
  }
}
