package io.iohk.atala.prism.management.console.repositories.metrics

import cats.effect.BracketThrow
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CredentialExternalId,
  ParticipantId,
  ReceivedSignedCredential
}
import io.iohk.atala.prism.management.console.repositories.ReceivedCredentialsRepository
import io.iohk.atala.prism.management.console.repositories.daos.ReceivedCredentialsDAO.ReceivedSignedCredentialData
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import tofu.higherKind.Mid

final class ReceivedCredentialsRepositoryMetrics[F[
    _
]: TimeMeasureMetric: BracketThrow]
    extends ReceivedCredentialsRepository[Mid[F, *]] {

  private val repoName = "ReceivedCredentialsRepository"
  private lazy val getCredentialsForTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getCredentialsFor")
  private lazy val createReceivedCredentialTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "createReceivedCredential")
  private lazy val getLatestCredentialExternalIdTimer =
    TimeMeasureUtil.createDBQueryTimer(
      repoName,
      "getLatestCredentialExternalId"
    )

  override def getCredentialsFor(
      verifierId: ParticipantId,
      contactId: Option[Contact.Id]
  ): Mid[F, List[ReceivedSignedCredential]] =
    _.measureOperationTime(getCredentialsForTimer)

  override def createReceivedCredential(
      data: ReceivedSignedCredentialData
  ): Mid[F, Unit] =
    _.measureOperationTime(createReceivedCredentialTimer)

  override def getLatestCredentialExternalId(
      verifierId: ParticipantId
  ): Mid[F, Option[CredentialExternalId]] =
    _.measureOperationTime(getLatestCredentialExternalIdTimer)
}
