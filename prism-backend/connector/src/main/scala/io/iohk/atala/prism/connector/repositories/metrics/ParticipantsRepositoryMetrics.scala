package io.iohk.atala.prism.connector.repositories.metrics

import io.iohk.atala.prism.connector.model.{ParticipantInfo, UpdateParticipantProfile}
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository._
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.models.ParticipantId
import tofu.higherKind.Mid
import cats.effect.MonadCancel

private[repositories] final class ParticipantsRepositoryMetrics[F[
    _
]: TimeMeasureMetric](implicit
    br: MonadCancel[F, Throwable]
) extends ParticipantsRepository[Mid[F, *]] {

  private val repoName = "ParticipantsRepository"
  private lazy val createTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "create")
  private lazy val findByIdTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "findById")
  private lazy val findByPublicKeyTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "findByPublicKey")
  private lazy val findByDidTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "findByDid")
  private lazy val updateParticipantProfileByTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "updateParticipantProfileBy")

  override def create(
      request: CreateParticipantRequest
  ): Mid[F, Either[CreateParticipantError, Unit]] =
    _.measureOperationTime(createTimer)

  override def findBy(
      id: ParticipantId
  ): Mid[F, Either[FindByError, ParticipantInfo]] =
    _.measureOperationTime(findByIdTimer)

  override def findBy(
      publicKey: ECPublicKey
  ): Mid[F, Either[FindByError, ParticipantInfo]] =
    _.measureOperationTime(findByPublicKeyTimer)

  override def findBy(did: DID): Mid[F, Either[FindByError, ParticipantInfo]] =
    _.measureOperationTime(findByDidTimer)

  override def updateParticipantProfileBy(
      id: ParticipantId,
      participantProfile: UpdateParticipantProfile
  ): Mid[F, Unit] = _.measureOperationTime(updateParticipantProfileByTimer)
}
