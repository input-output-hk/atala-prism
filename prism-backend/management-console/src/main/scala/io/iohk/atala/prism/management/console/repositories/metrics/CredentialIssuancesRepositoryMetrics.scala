package io.iohk.atala.prism.management.console.repositories.metrics

import cats.effect.BracketThrow
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models.{CredentialIssuance, CredentialTypeId, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.CredentialIssuancesRepository
import io.iohk.atala.prism.management.console.repositories.CredentialIssuancesRepository.{
  CreateCredentialBulk,
  CreateCredentialIssuance
}
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import tofu.higherKind.Mid

final class CredentialIssuancesRepositoryMetrics[F[
    _
]: TimeMeasureMetric: BracketThrow]
    extends CredentialIssuancesRepository[Mid[F, *]] {

  private val repoName = "CredentialIssuancesRepository"
  private lazy val createTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "create")
  private lazy val createBulkTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "createBulk")
  private lazy val getTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "get")

  override def create(
      participantId: ParticipantId,
      createCredentialIssuance: CreateCredentialIssuance
  ): Mid[F, Either[ManagementConsoleError, CredentialIssuance.Id]] =
    _.measureOperationTime(createTimer)

  override def createBulk(
      participantId: ParticipantId,
      credentialsType: CredentialTypeId,
      issuanceName: String,
      drafts: List[CreateCredentialBulk.Draft]
  ): Mid[F, Either[ManagementConsoleError, CredentialIssuance.Id]] =
    _.measureOperationTime(createBulkTimer)

  override def get(
      credentialIssuanceId: CredentialIssuance.Id,
      institutionId: ParticipantId
  ): Mid[F, CredentialIssuance] = _.measureOperationTime(getTimer)
}
