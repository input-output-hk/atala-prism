package io.iohk.atala.prism.connector.repositories.metrics

import cats.effect.Bracket
import io.iohk.atala.prism.connector.errors.ConnectorError
import io.iohk.atala.prism.connector.model.{ParticipantInfo, UpdateParticipantProfile}
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository.CreateParticipantRequest
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.models.ParticipantId
import tofu.higherKind.Mid

private[repositories] final class ParticipantsRepositoryMetrics[F[_]: TimeMeasureMetric](implicit
    br: Bracket[F, Throwable]
) extends ParticipantsRepository[Mid[F, *]] {

  private val repoName = "ParticipantsRepository"
  private lazy val createTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "create")
  private lazy val findByIdTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "findById")
  private lazy val findByPublicKeyTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "findByPublicKey")
  private lazy val findByDidTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "findByDid")
  private lazy val updateParticipantProfileByTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "updateParticipantProfileBy")

  override def create(request: CreateParticipantRequest): Mid[F, Either[ConnectorError, Unit]] =
    _.measureOperationTime(createTimer)

  override def findBy(id: ParticipantId): Mid[F, Either[ConnectorError, ParticipantInfo]] =
    _.measureOperationTime(findByIdTimer)

  override def findBy(publicKey: ECPublicKey): Mid[F, Either[ConnectorError, ParticipantInfo]] =
    _.measureOperationTime(findByPublicKeyTimer)

  override def findBy(did: DID): Mid[F, Either[ConnectorError, ParticipantInfo]] =
    _.measureOperationTime(findByDidTimer)

  override def updateParticipantProfileBy(
      id: ParticipantId,
      participantProfile: UpdateParticipantProfile
  ): Mid[F, Unit] = _.measureOperationTime(updateParticipantProfileByTimer)
}
