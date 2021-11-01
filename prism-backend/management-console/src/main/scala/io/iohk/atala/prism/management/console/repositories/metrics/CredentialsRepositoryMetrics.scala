package io.iohk.atala.prism.management.console.repositories.metrics

import cats.data.NonEmptyList
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models.GenericCredential.PaginatedQuery
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.CredentialsRepository
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import tofu.higherKind.Mid
import cats.effect.MonadCancelThrow

final class CredentialsRepositoryMetrics[F[_]: TimeMeasureMetric: MonadCancelThrow]
    extends CredentialsRepository[Mid[F, *]] {

  private val repoName = "CredentialsRepository"
  private lazy val createTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "create")
  private lazy val getByCredentialIdTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getByCredentialId")
  private lazy val getByQueryTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getByQuery")
  private lazy val getByMaybeLastSeenCredentialTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getByMaybeLastSeenCredential")
  private lazy val getByContactIdTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getByContactId")
  private lazy val storePublicationDataTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "storePublicationData")
  private lazy val markAsSharedTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "markAsShared")
  private lazy val verifyPublishedCredentialsExistTimer =
    TimeMeasureUtil.createDBQueryTimer(
      repoName,
      "verifyPublishedCredentialsExist"
    )
  private lazy val storeBatchDataTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "storeBatchData")
  private lazy val deleteCredentialsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "deleteCredentials")
  private lazy val storeRevocationDataTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "storeRevocationData")

  override def create(
      participantId: ParticipantId,
      data: CreateGenericCredential
  ): Mid[F, Either[ManagementConsoleError, GenericCredential]] =
    _.measureOperationTime(createTimer)

  override def getBy(
      credentialId: GenericCredential.Id
  ): Mid[F, Option[GenericCredential]] =
    _.measureOperationTime(getByCredentialIdTimer)

  override def getBy(
      issuedBy: ParticipantId,
      query: PaginatedQuery
  ): Mid[F, List[GenericCredential]] =
    _.measureOperationTime(getByQueryTimer)

  override def getBy(
      issuedBy: ParticipantId,
      limit: Int,
      lastSeenCredential: Option[GenericCredential.Id]
  ): Mid[F, List[GenericCredential]] =
    _.measureOperationTime(getByMaybeLastSeenCredentialTimer)

  override def getBy(
      issuedBy: ParticipantId,
      contactId: Contact.Id
  ): Mid[F, List[GenericCredential]] =
    _.measureOperationTime(getByContactIdTimer)

  override def storePublicationData(
      issuerId: ParticipantId,
      credentialData: PublishCredential
  ): Mid[F, Int] =
    _.measureOperationTime(storePublicationDataTimer)

  override def markAsShared(
      issuerId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): Mid[F, Unit] =
    _.measureOperationTime(markAsSharedTimer)

  override def verifyPublishedCredentialsExist(
      issuerId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    _.measureOperationTime(verifyPublishedCredentialsExistTimer)

  override def storeBatchData(
      batchId: CredentialBatchId,
      issuanceOperationHash: Sha256Digest,
      atalaOperationId: AtalaOperationId
  ): Mid[F, Int] = _.measureOperationTime(storeBatchDataTimer)

  override def deleteCredentials(
      institutionId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    _.measureOperationTime(deleteCredentialsTimer)

  override def storeRevocationData(
      institutionId: ParticipantId,
      credentialId: GenericCredential.Id,
      operationId: AtalaOperationId
  ): Mid[F, Unit] = _.measureOperationTime(storeRevocationDataTimer)
}
