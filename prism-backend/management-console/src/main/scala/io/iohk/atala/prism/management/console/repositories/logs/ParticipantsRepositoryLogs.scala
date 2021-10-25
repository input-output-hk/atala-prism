package io.iohk.atala.prism.management.console.repositories.logs

import cats.effect.BracketThrow
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.management.console.errors
import io.iohk.atala.prism.management.console.models.{ParticipantId, ParticipantInfo}
import io.iohk.atala.prism.management.console.repositories.ParticipantsRepository
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

private[repositories] final class ParticipantsRepositoryLogs[F[
    _
]: BracketThrow: ServiceLogging[*[
  _
], ParticipantsRepository[F]]]
    extends ParticipantsRepository[Mid[F, *]] {
  override def create(
      request: ParticipantsRepository.CreateParticipantRequest
  ): Mid[F, Either[errors.ManagementConsoleError, Unit]] =
    in =>
      info"creating participant ${request.id}" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while creating participant $e",
            r => info"creating participant - successfully done $r"
          )
        )
        .onError(
          errorCause"encountered an error while creating participant" (_)
        )

  override def findBy(
      id: ParticipantId
  ): Mid[F, Either[errors.ManagementConsoleError, ParticipantInfo]] =
    in =>
      info"finding participant $id" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while finding participant $e",
            r => info"finding participant - successfully done ${r.id}"
          )
        )
        .onError(errorCause"encountered an error while finding participant" (_))

  override def findBy(
      did: DID
  ): Mid[F, Either[errors.ManagementConsoleError, ParticipantInfo]] =
    in =>
      info"finding participant ${Option(did.asCanonical().getSuffix)}" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while finding participant $e",
            r => info"finding participant - successfully done ${r.id}"
          )
        )
        .onError(errorCause"encountered an error while finding participant" (_))

  override def update(
      request: ParticipantsRepository.UpdateParticipantProfileRequest
  ): Mid[F, Unit] =
    in =>
      info"updating participant ${request.id}" *> in
        .flatTap(_ => info"updating participant - successfully done")
        .onError(
          errorCause"encountered an error while updating participant" (_)
        )
}
