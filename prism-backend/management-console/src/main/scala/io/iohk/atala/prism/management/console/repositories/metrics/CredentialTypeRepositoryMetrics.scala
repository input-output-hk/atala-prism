package io.iohk.atala.prism.management.console.repositories.metrics

import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.CredentialTypeRepository
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import tofu.higherKind.Mid
import cats.effect.MonadCancelThrow

final class CredentialTypeRepositoryMetrics[F[
    _
]: TimeMeasureMetric: MonadCancelThrow]
    extends CredentialTypeRepository[Mid[F, *]] {
  private val repoName = "CredentialTypeRepository"
  private lazy val createTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "create")
  private lazy val updateTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "update")
  private lazy val markAsArchivedTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "markAsArchived")
  private lazy val markAsReadyTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "markAsReady")
  private lazy val findByCredTypeTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "findByCredentialType")
  private lazy val findByNameTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "findByName")
  private lazy val findByCredTypeAndInstitutionIdTimer =
    TimeMeasureUtil.createDBQueryTimer(
      repoName,
      "findByCredTypeAndInstitutionId"
    )
  private lazy val findByInstitutionIdTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "findByInstitutionId")

  override def create(
      participantId: ParticipantId,
      createCredentialType: CreateCredentialType
  ): Mid[F, Either[ManagementConsoleError, CredentialTypeWithRequiredFields]] =
    _.measureOperationTime(createTimer)

  override def update(
      updateCredentialType: UpdateCredentialType,
      institutionId: ParticipantId
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    _.measureOperationTime(updateTimer)

  override def markAsArchived(
      credentialTypeId: CredentialTypeId,
      institutionId: ParticipantId
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    _.measureOperationTime(markAsArchivedTimer)

  override def markAsReady(
      credentialTypeId: CredentialTypeId,
      institutionId: ParticipantId
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    _.measureOperationTime(markAsReadyTimer)

  override def find(
      credentialTypeId: CredentialTypeId
  ): Mid[F, Option[CredentialTypeWithRequiredFields]] =
    _.measureOperationTime(findByCredTypeTimer)

  override def find(
      institution: ParticipantId,
      name: String
  ): Mid[F, Option[CredentialTypeWithRequiredFields]] =
    _.measureOperationTime(findByNameTimer)

  override def find(
      institution: ParticipantId,
      credentialTypeId: CredentialTypeId
  ): Mid[F, Option[CredentialTypeWithRequiredFields]] =
    _.measureOperationTime(findByCredTypeAndInstitutionIdTimer)

  override def findByInstitution(
      institution: ParticipantId
  ): Mid[F, List[CredentialType]] =
    _.measureOperationTime(findByInstitutionIdTimer)
}
