package io.iohk.atala.prism.management.console.repositories.metrics

import cats.effect.BracketThrow
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models.{ParticipantId, ParticipantInfo}
import io.iohk.atala.prism.management.console.repositories.ParticipantsRepository
import io.iohk.atala.prism.management.console.repositories.ParticipantsRepository.{
  CreateParticipantRequest,
  UpdateParticipantProfileRequest
}
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import tofu.higherKind.Mid

final class ParticipantsRepositoryMetrics[F[_]: TimeMeasureMetric: BracketThrow]
    extends ParticipantsRepository[Mid[F, *]] {

  private val repoName = "ParticipantsRepository"
  private lazy val createTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "create")
  private lazy val findByParticipantIdTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "findByParticipantId")
  private lazy val findByDidTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "findByDid")
  private lazy val updateTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "update")

  override def create(
      request: CreateParticipantRequest
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    _.measureOperationTime(createTimer)

  override def findBy(
      id: ParticipantId
  ): Mid[F, Either[ManagementConsoleError, ParticipantInfo]] =
    _.measureOperationTime(findByParticipantIdTimer)

  override def findBy(
      did: DID
  ): Mid[F, Either[ManagementConsoleError, ParticipantInfo]] =
    _.measureOperationTime(findByDidTimer)

  override def update(request: UpdateParticipantProfileRequest): Mid[F, Unit] =
    _.measureOperationTime(updateTimer)
}
