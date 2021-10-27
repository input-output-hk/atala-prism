package io.iohk.atala.prism.management.console.repositories.metrics

import cats.effect.BracketThrow
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models.Contact.PaginatedQuery
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.ContactsRepository
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.models.ConnectionToken
import tofu.higherKind.Mid

import java.time.Instant

final class ContactsRepositoryMetrics[F[_]: TimeMeasureMetric: BracketThrow] extends ContactsRepository[Mid[F, *]] {

  private val repoName = "ContactsRepository"
  private lazy val createTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "create")
  private lazy val createBatchTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "createBatch")
  private lazy val updateContactTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "updateContact")
  private lazy val findByContactIdTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "findByContactId")
  private lazy val findByExternalIdTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "findByExternalId")
  private lazy val findByTokenTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "findByToken")
  private lazy val findContactsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "findContacts")
  private lazy val getByTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getBy")
  private lazy val deleteTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "delete")

  override def create(
      participantId: ParticipantId,
      contactData: CreateContact,
      maybeGroupName: Option[InstitutionGroup.Name],
      createdAt: Instant,
      connectionToken: ConnectionToken
  ): Mid[F, Contact] = _.measureOperationTime(createTimer)

  override def createBatch(
      institutionId: ParticipantId,
      request: CreateContact.Batch,
      connectionTokens: List[ConnectionToken]
  ): Mid[F, Either[ManagementConsoleError, Int]] =
    _.measureOperationTime(createBatchTimer)

  override def updateContact(
      institutionId: ParticipantId,
      request: UpdateContact
  ): Mid[F, Unit] =
    _.measureOperationTime(updateContactTimer)

  override def find(
      institutionId: ParticipantId,
      contactId: Contact.Id
  ): Mid[F, Option[Contact.WithDetails]] =
    _.measureOperationTime(findByContactIdTimer)

  override def find(
      institutionId: ParticipantId,
      externalId: Contact.ExternalId
  ): Mid[F, Option[Contact]] =
    _.measureOperationTime(findByExternalIdTimer)

  override def findByToken(
      institutionId: ParticipantId,
      connectionToken: ConnectionToken
  ): Mid[F, Option[Contact]] =
    _.measureOperationTime(findByTokenTimer)

  override def findContacts(
      institutionId: ParticipantId,
      contactIds: List[Contact.Id]
  ): Mid[F, List[Contact]] =
    _.measureOperationTime(findContactsTimer)

  override def getBy(
      createdBy: ParticipantId,
      constraints: PaginatedQuery,
      ignoreFilterLimit: Boolean
  ): Mid[F, List[Contact.WithCredentialCounts]] =
    _.measureOperationTime(getByTimer)

  override def delete(
      institutionId: ParticipantId,
      contactId: Contact.Id,
      deleteCredentials: Boolean
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    _.measureOperationTime(deleteTimer)
}
