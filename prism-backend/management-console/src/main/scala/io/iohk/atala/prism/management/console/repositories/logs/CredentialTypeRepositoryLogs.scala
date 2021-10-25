package io.iohk.atala.prism.management.console.repositories.logs

import cats.effect.BracketThrow
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.CredentialTypeRepository
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

private[repositories] final class CredentialTypeRepositoryLogs[F[
    _
]: ServiceLogging[
  *[_],
  CredentialTypeRepository[F]
]: BracketThrow]
    extends CredentialTypeRepository[Mid[F, *]] {

  override def create(
      participantId: ParticipantId,
      createCredentialType: CreateCredentialType
  ): Mid[F, Either[ManagementConsoleError, CredentialTypeWithRequiredFields]] =
    in =>
      info"creating credential type $participantId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while creating credential type $e",
            r => info"creating credential type - successfully done ${r.credentialType.id}"
          )
        )
        .onError(
          errorCause"encountered an error while creating credential type" (_)
        )

  override def update(
      updateCredentialType: UpdateCredentialType,
      institutionId: ParticipantId
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"updating credential type $institutionId ${updateCredentialType.id}" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while updating credential type $e",
            _ => info"updating credential type - successfully done"
          )
        )
        .onError(
          errorCause"encountered an error while updating credential type" (_)
        )

  override def markAsArchived(
      credentialTypeId: CredentialTypeId,
      institutionId: ParticipantId
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"marking as archived $institutionId $credentialTypeId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while marking as archived credential type $e",
            _ => info"marking as archived - successfully done"
          )
        )
        .onError(errorCause"encountered an error while marking as archived" (_))

  override def markAsReady(
      credentialTypeId: CredentialTypeId,
      institutionId: ParticipantId
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"marking as ready $institutionId $credentialTypeId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while marking as ready credential type $e",
            _ => info"marking as ready - successfully done"
          )
        )
        .onError(errorCause"encountered an error while marking as ready" (_))

  override def find(
      credentialTypeId: CredentialTypeId
  ): Mid[F, Option[CredentialTypeWithRequiredFields]] =
    in =>
      info"finding by credential type id $credentialTypeId" *> in
        .flatTap(result => info"finding by credential type id - ${result.fold("found nothing")(_ => "found")}")
        .onError(
          errorCause"encountered an error while finding by credential type id" (
            _
          )
        )

  override def find(
      institution: ParticipantId,
      name: String
  ): Mid[F, Option[CredentialTypeWithRequiredFields]] =
    in =>
      info"finding by institution id and name $institution $name" *> in
        .flatTap(result =>
          info"finding by institution id and name - ${result
            .fold("found nothing")(_ => "found")}"
        )
        .onError(
          errorCause"encountered an error while finding by institution id and name" (
            _
          )
        )

  override def find(
      institution: ParticipantId,
      credentialTypeId: CredentialTypeId
  ): Mid[F, Option[CredentialTypeWithRequiredFields]] =
    in =>
      info"finding by institution id and credential type id $institution $credentialTypeId" *> in
        .flatTap(result =>
          info"finding by institution id and credential type id - ${result
            .fold("found nothing")(_ => "found")}"
        )
        .onError(
          errorCause"encountered an error while finding by institution id and credential type id" (
            _
          )
        )

  override def findByInstitution(
      institution: ParticipantId
  ): Mid[F, List[CredentialType]] =
    in =>
      info"finding by institution id $institution" *> in
        .flatTap(result => info"finding by institution id - found ${result.size} entities")
        .onError(
          errorCause"encountered an error while finding by institution id" (_)
        )
}
