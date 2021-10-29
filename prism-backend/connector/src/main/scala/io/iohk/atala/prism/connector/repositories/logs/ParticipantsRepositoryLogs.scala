package io.iohk.atala.prism.connector.repositories.logs

import cats.effect.MonadThrow
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.connector.errors.ConnectorError
import io.iohk.atala.prism.connector.model.{ParticipantInfo, UpdateParticipantProfile}
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository._
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.logging.GeneralLoggableInstances._
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

private[repositories] final class ParticipantsRepositoryLogs[F[
    _
]: ServiceLogging[
  *[_],
  ParticipantsRepository[F]
]: MonadThrow]
    extends ParticipantsRepository[Mid[F, *]] {
  override def create(
      request: ParticipantsRepository.CreateParticipantRequest
  ): Mid[F, Either[CreateParticipantError, Unit]] =
    in =>
      info"creating participant ${request.id}" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while creating participant ${er.unify: ConnectorError}",
            _ => info"creating participant - successfully done"
          )
        )
        .onError(
          errorCause"encountered an error while creating participant" (_)
        )

  override def findBy(
      id: ParticipantId
  ): Mid[F, Either[FindByError, ParticipantInfo]] =
    in =>
      info"finding participant $id" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while finding participant ${er.unify: ConnectorError}",
            _ => info"finding participant  - successfully done"
          )
        )
        .onError(errorCause"encountered an error while finding participant" (_))

  override def findBy(
      publicKey: ECPublicKey
  ): Mid[F, Either[FindByError, ParticipantInfo]] =
    in =>
      info"finding participant by public-key $publicKey" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while finding participant ${er.unify: ConnectorError}",
            result => info"finding participant - successfully done ${result.id}"
          )
        )
        .onError(
          errorCause"encountered an error while finding participant by public-key" (
            _
          )
        )

  override def findBy(did: DID): Mid[F, Either[FindByError, ParticipantInfo]] =
    in =>
      info"finding participant by did ${did.getSuffix}" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while finding participant ${er.unify: ConnectorError}",
            result => info"finding participant - successfully done ${result.id}"
          )
        )
        .onError(
          errorCause"encountered an error while finding participant by did" (_)
        )

  override def updateParticipantProfileBy(
      id: ParticipantId,
      participantProfile: UpdateParticipantProfile
  ): Mid[F, Unit] =
    in =>
      info"updating participant profile $id" *> in
        .flatTap(_ => info"updating participant profile - successfully done")
        .onError(
          errorCause"encountered an error while updating participant profile" (
            _
          )
        )
}
