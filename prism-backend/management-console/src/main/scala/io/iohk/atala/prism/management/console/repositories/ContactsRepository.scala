package io.iohk.atala.prism.management.console.repositories

import cats.{Comonad, Functor, Monad}
import cats.data.OptionT
import cats.data.EitherT
import cats.effect._
import cats.implicits._
import derevo.tagless.applyK
import derevo.derive
import doobie.free._
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.daos._
import io.iohk.atala.prism.management.console.repositories.logs.ContactsRepositoryLogs
import io.iohk.atala.prism.management.console.repositories.metrics.ContactsRepositoryMetrics
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.models.ConnectionToken
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps

import java.time.Instant
import cats.effect.MonadCancelThrow

@derive(applyK)
trait ContactsRepository[F[_]] {

  def create(
      participantId: ParticipantId,
      contactData: CreateContact,
      maybeGroupName: Option[InstitutionGroup.Name],
      createdAt: Instant = Instant.now(),
      connectionToken: ConnectionToken
  ): F[Contact]

  def createBatch(
      institutionId: ParticipantId,
      request: CreateContact.Batch,
      connectionTokens: List[ConnectionToken]
  ): F[Either[ManagementConsoleError, Int]]

  def updateContact(
      institutionId: ParticipantId,
      request: UpdateContact
  ): F[Unit]

  def find(
      institutionId: ParticipantId,
      contactId: Contact.Id
  ): F[Option[Contact.WithDetails]]

  def find(
      institutionId: ParticipantId,
      externalId: Contact.ExternalId
  ): F[Option[Contact]]

  def findByToken(
      institutionId: ParticipantId,
      connectionToken: ConnectionToken
  ): F[Option[Contact]]

  def findContacts(
      institutionId: ParticipantId,
      contactIds: List[Contact.Id]
  ): F[List[Contact]]

  def getBy(
      createdBy: ParticipantId,
      constraints: Contact.PaginatedQuery,
      ignoreFilterLimit: Boolean = false
  ): F[List[Contact.WithCredentialCounts]]

  def delete(
      institutionId: ParticipantId,
      contactId: Contact.Id,
      deleteCredentials: Boolean
  ): F[Either[ManagementConsoleError, Unit]]

}

object ContactsRepository {

  def apply[F[_]: TimeMeasureMetric: MonadCancelThrow, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[ContactsRepository[F]] =
    for {
      serviceLogs <- logs.service[ContactsRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, ContactsRepository[F]] =
        serviceLogs
      val metrics: ContactsRepository[Mid[F, *]] =
        new ContactsRepositoryMetrics[F]
      val logs: ContactsRepository[Mid[F, *]] = new ContactsRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new ContactsRepositoryImpl[F](transactor)
    }

  def unsafe[F[_]: TimeMeasureMetric: MonadCancelThrow, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): ContactsRepository[F] = ContactsRepository(transactor, logs).extract

  def makeResource[F[_]: TimeMeasureMetric: MonadCancelThrow, R[_]: Monad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, ContactsRepository[F]] =
    Resource.eval(ContactsRepository(transactor, logs))

}

private final class ContactsRepositoryImpl[F[_]: MonadCancelThrow](
    xa: Transactor[F]
) extends ContactsRepository[F] {
  def create(
      participantId: ParticipantId,
      contactData: CreateContact,
      maybeGroupName: Option[InstitutionGroup.Name],
      createdAt: Instant = Instant.now(),
      connectionToken: ConnectionToken
  ): F[Contact] = {
    val query = maybeGroupName match {
      case None => // if we do not request the contact to be added to a group
        ContactsDAO.createContact(
          participantId,
          contactData,
          createdAt,
          connectionToken
        )
      case Some(
            groupName
          ) => // if we are requesting to add a contact to a group
        for {
          contact <- ContactsDAO.createContact(
            participantId,
            contactData,
            createdAt,
            connectionToken
          )
          groupMaybe <- InstitutionGroupsDAO.find(participantId, groupName)
          group = groupMaybe.getOrElse(
            throw new RuntimeException(s"Group $groupName does not exist")
          )
          _ <- InstitutionGroupsDAO.addContact(group.id, contact.contactId)
        } yield contact
    }

    query
      .logSQLErrorsV2(s"creating contact, participant id - $participantId")
      .transact(xa)
  }

  def createBatch(
      institutionId: ParticipantId,
      request: CreateContact.Batch,
      connectionTokens: List[ConnectionToken]
  ): F[Either[ManagementConsoleError, Int]] = {
    def unsafe = {
      for {
        contactIds <- ContactsDAO.createContacts(
          institutionId,
          request.contacts,
          Instant.now(),
          connectionTokens
        )
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
      .logSQLErrorsV2(s"creating contacts by batch, institution id - $institutionId")
      .transact(xa)
  }

  def updateContact(
      institutionId: ParticipantId,
      request: UpdateContact
  ): F[Unit] =
    ContactsDAO
      .updateContact(institutionId, request)
      .logSQLErrorsV2(s"updating contact, institution id - $institutionId")
      .transact(xa)

  def find(
      institutionId: ParticipantId,
      contactId: Contact.Id
  ): F[Option[Contact.WithDetails]] =
    (for {
      contact <- OptionT(ContactsDAO.findContact(institutionId, contactId))
      institutionsInvolved <- OptionT.liftF(
        InstitutionGroupsDAO.getBy(institutionId, contactId)
      )
      receivedCredentials <-
        OptionT.liftF(
          ReceivedCredentialsDAO.getReceivedCredentialsFor(
            institutionId,
            Some(contactId)
          )
        )
      issuedCredentials <- OptionT.liftF(
        CredentialsDAO.getIssuedCredentialsBy(institutionId, contactId)
      )
    } yield Contact.WithDetails(
      contact,
      institutionsInvolved,
      receivedCredentials,
      issuedCredentials
    )).value
      .logSQLErrorsV2(s"finding contact with details, contact id - $contactId")
      .transact(xa)

  def find(
      institutionId: ParticipantId,
      externalId: Contact.ExternalId
  ): F[Option[Contact]] =
    ContactsDAO
      .findContact(institutionId, externalId)
      .logSQLErrorsV2(s"finding contact, institution id - $institutionId")
      .transact(xa)

  override def findByToken(
      institutionId: ParticipantId,
      connectionToken: ConnectionToken
  ): F[Option[Contact]] =
    ContactsDAO
      .findByToken(institutionId, connectionToken)
      .logSQLErrorsV2(s"finding contact by token, institution id - $institutionId, token = $connectionToken")
      .transact(xa)

  def findContacts(
      institutionId: ParticipantId,
      contactIds: List[Contact.Id]
  ): F[List[Contact]] =
    ContactsDAO
      .findContacts(institutionId, contactIds)
      .logSQLErrorsV2(s"finding contacts, institution id - $institutionId")
      .transact(xa)

  def getBy(
      createdBy: ParticipantId,
      constraints: Contact.PaginatedQuery,
      ignoreFilterLimit: Boolean
  ): F[List[Contact.WithCredentialCounts]] =
    ContactsDAO
      .getBy(createdBy, constraints, ignoreFilterLimit)
      .logSQLErrorsV2(s"getting by some constraint, created by - $createdBy")
      .transact(xa)

  def delete(
      institutionId: ParticipantId,
      contactId: Contact.Id,
      deleteCredentials: Boolean
  ): F[Either[ManagementConsoleError, Unit]] = {
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
        _ = if (!isDeleted)
          throw new IllegalStateException(
            "The requested contact was not deleted"
          )
      } yield Right(())

    val connectionIO = for {
      // Check that the contact belongs to the correct institution
      _ <- EitherT(
        institutionHelper
          .checkContacts(institutionId, Set(contactId))
          .map(_.toLeft(()))
      )
      _ <-
        if (deleteCredentials) {
          EitherT.rightT[ConnectionIO, ManagementConsoleError][Unit](())
        } else {
          // Check if we are trying to delete a contact without deleting its existing credentials
          EitherT(
            institutionHelper
              .checkCredentialsAreEmpty(institutionId, contactId)
              .map(_.toLeft(()))
          )
        }
      // All checks have passed, we can actually start deleting now
      result <- EitherT(performDeletion())
    } yield result

    connectionIO.value
      .logSQLErrorsV2(s"deleting contact, institution id - $institutionId")
      .transact(xa)
  }
}
