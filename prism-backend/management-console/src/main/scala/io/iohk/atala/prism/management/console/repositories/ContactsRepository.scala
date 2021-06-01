package io.iohk.atala.prism.management.console.repositories

import cats.data.OptionT
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
import io.iohk.atala.prism.management.console.repositories.daos.{
  ContactsDAO,
  CredentialsDAO,
  InstitutionGroupsDAO,
  ReceivedCredentialsDAO
}
import io.iohk.atala.prism.models.ConnectionToken
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}

import java.time.Instant
import scala.concurrent.ExecutionContext

class ContactsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def create(
      participantId: ParticipantId,
      contactData: CreateContact,
      maybeGroupName: Option[InstitutionGroup.Name],
      createdAt: Instant = Instant.now(),
      connectionToken: ConnectionToken
  ): FutureEither[ManagementConsoleError, Contact] = {
    val query = maybeGroupName match {
      case None => // if we do not request the contact to be added to a group
        ContactsDAO.createContact(participantId, contactData, createdAt, connectionToken)
      case Some(groupName) => // if we are requesting to add a contact to a group
        for {
          contact <- ContactsDAO.createContact(participantId, contactData, createdAt, connectionToken)
          groupMaybe <- InstitutionGroupsDAO.find(participantId, groupName)
          group = groupMaybe.getOrElse(throw new RuntimeException(s"Group $groupName does not exist"))
          _ <- InstitutionGroupsDAO.addContact(group.id, contact.contactId)
        } yield contact
    }

    query
      .logSQLErrors(s"creating contact, participant id - $participantId", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def createBatch(
      institutionId: ParticipantId,
      request: CreateContact.Batch,
      connectionTokens: List[ConnectionToken]
  ): FutureEither[ManagementConsoleError, Int] = {
    def unsafe = {
      for {
        contactIds <- ContactsDAO.createContacts(institutionId, request.contacts, Instant.now(), connectionTokens)
        _ <- InstitutionGroupsDAO.addContacts(request.groups, contactIds.toSet)
      } yield contactIds.size.asRight[ManagementConsoleError]
    }

    val connectionIO = for {
      errorMaybe <- institutionHelper.checkGroups(institutionId, request.groups)
      result <- errorMaybe match {
        case Some(consoleError) => connection.pure(consoleError.asLeft[Int])
        case None => unsafe
      }
    } yield result

    connectionIO
      .logSQLErrors(s"creating contacts by batch, institution id - $institutionId", logger)
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither
  }

  def updateContact(institutionId: ParticipantId, request: UpdateContact): FutureEither[Nothing, Unit] = {
    ContactsDAO
      .updateContact(institutionId, request)
      .logSQLErrors(s"updating contact, institution id - $institutionId", logger)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }

  def find(institutionId: ParticipantId, contactId: Contact.Id): FutureEither[Nothing, Option[Contact.WithDetails]] = {
    (for {
      contact <- OptionT(ContactsDAO.findContact(institutionId, contactId))
      institutionsInvolved <- OptionT.liftF(InstitutionGroupsDAO.getBy(institutionId, contactId))
      receivedCredentials <- OptionT.liftF(ReceivedCredentialsDAO.getReceivedCredentialsFor(institutionId, contactId))
      issuedCredentials <- OptionT.liftF(CredentialsDAO.getIssuedCredentialsBy(institutionId, contactId))
    } yield Contact.WithDetails(contact, institutionsInvolved, receivedCredentials, issuedCredentials)).value
      .logSQLErrors(s"finding contact with details, contact id - $contactId", logger)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }

  def find(institutionId: ParticipantId, externalId: Contact.ExternalId): FutureEither[Nothing, Option[Contact]] = {
    ContactsDAO
      .findContact(institutionId, externalId)
      .logSQLErrors(s"finding contact, institution id - $institutionId", logger)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }

  def findContacts(institutionId: ParticipantId, contactIds: List[Contact.Id]): FutureEither[Nothing, List[Contact]] = {
    ContactsDAO
      .findContacts(institutionId, contactIds)
      .logSQLErrors(s"finding contacts, institution id - $institutionId", logger)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }

  def getBy(
      createdBy: ParticipantId,
      constraints: Contact.PaginatedQuery,
      ignoreFilterLimit: Boolean = false
  ): FutureEither[ManagementConsoleError, Seq[Contact.WithCredentialCounts]] = {
    ContactsDAO
      .getBy(createdBy, constraints, ignoreFilterLimit)
      .logSQLErrors(s"getting by some constraint, created by - $createdBy", logger)
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
      .logSQLErrors(s"deleting contact, institution id - $institutionId", logger)
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither
  }
}
