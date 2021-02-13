package io.iohk.atala.prism.management.console.repositories

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
import io.iohk.atala.prism.management.console.repositories.daos.{ContactsDAO, InstitutionGroupsDAO}
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
      case None => // if we do not request the subject to be added to a group
        ContactsDAO.createContact(contactData, createdAt)
      case Some(groupName) => // if we are requesting to add a subject to a group
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

  def find(institutionId: ParticipantId, subjectId: Contact.Id): FutureEither[Nothing, Option[Contact]] = {
    ContactsDAO
      .findContact(institutionId, subjectId)
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
}
