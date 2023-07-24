package io.iohk.atala.prism.management.console.repositories.metrics

import cats.effect.MonadCancelThrow
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.CredentialTypeCategoryRepository
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import tofu.higherKind.Mid

final class CredentialTypeCategoryRepositoryMetrics[F[
    _
]: TimeMeasureMetric: MonadCancelThrow]
    extends CredentialTypeCategoryRepository[Mid[F, *]] {
  private val repoName = "CredentialTypeCategoryRepository"
  private lazy val createTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "create")
  private lazy val archiveTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "archive")
  private lazy val unArchiveTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "unArchive")
  private lazy val findByInstitutionTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "findByInstitution")

  override def create(
      participantId: ParticipantId,
      createCredentialTypeCategory: CreateCredentialTypeCategory
  ): Mid[F, Either[ManagementConsoleError, CredentialTypeCategory]] =
    _.measureOperationTime(createTimer)

  override def archive(
      credentialTypeId: CredentialTypeCategoryId
  ): Mid[F, Either[ManagementConsoleError, CredentialTypeCategory]] =
    _.measureOperationTime(archiveTimer)

  override def unArchive(
      credentialTypeId: CredentialTypeCategoryId
  ): Mid[F, Either[ManagementConsoleError, CredentialTypeCategory]] = _.measureOperationTime(unArchiveTimer)

  override def findByInstitution(
      institution: ParticipantId
  ): Mid[F, Either[ManagementConsoleError, List[CredentialTypeCategory]]] =
    _.measureOperationTime(findByInstitutionTimer)

}
