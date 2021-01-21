package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models.{Contact, CreateContact, InstitutionGroup, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.daos.{ContactsDAO, InstitutionGroupsDAO}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class ContactsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(
      contactData: CreateContact,
      maybeGroupName: Option[InstitutionGroup.Name]
  ): FutureEither[ManagementConsoleError, Contact] = {
    val query = maybeGroupName match {
      case None => // if we do not request the subject to be added to a group
        ContactsDAO.createContact(contactData)
      case Some(groupName) => // if we are requesting to add a subject to a group
        for {
          contact <- ContactsDAO.createContact(contactData)
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
      scrollId: Option[Contact.Id],
      groupName: Option[InstitutionGroup.Name],
      limit: Int
  ): FutureEither[ManagementConsoleError, Seq[Contact]] = {
    ContactsDAO
      .getBy(createdBy, scrollId, limit, groupName)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
