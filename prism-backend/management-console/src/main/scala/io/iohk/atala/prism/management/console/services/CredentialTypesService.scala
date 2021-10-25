package io.iohk.atala.prism.management.console.services

import cats.{Comonad, Functor, Monad}
import cats.effect.{BracketThrow, MonadThrow, Resource}
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.comonad._
import cats.syntax.functor._
import cats.syntax.flatMap._
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.management.console.repositories.CredentialTypeRepository
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.logging._

@derive(applyK)
trait CredentialTypesService[F[_]] {

  def getCredentialTypes(institution: ParticipantId): F[List[CredentialType]]

  def getCredentialType(
      institution: ParticipantId,
      getCredentialType: GetCredentialType
  ): F[Option[CredentialTypeWithRequiredFields]]

  def createCredentialType(
      participantId: ParticipantId,
      createCredentialType: CreateCredentialType
  ): F[Either[ManagementConsoleError, CredentialTypeWithRequiredFields]]

  def updateCredentialType(
      institutionId: ParticipantId,
      updateCredentialType: UpdateCredentialType
  ): F[Either[ManagementConsoleError, Unit]]

  def markAsReady(
      participantId: ParticipantId,
      markAsReadyCredentialType: MarkAsReadyCredentialType
  ): F[Either[ManagementConsoleError, Unit]]

  def markAsArchived(
      participantId: ParticipantId,
      credentialTypeId: CredentialTypeId
  ): F[Either[ManagementConsoleError, Unit]]

}

object CredentialTypesService {
  def apply[F[_]: BracketThrow, R[_]: Functor](
      credentialTypeRepository: CredentialTypeRepository[F],
      logs: Logs[R, F]
  ): R[CredentialTypesService[F]] =
    for {
      serviceLogs <- logs.service[CredentialTypesService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, CredentialTypesService[F]] =
        serviceLogs
      val logs: CredentialTypesService[Mid[F, *]] =
        new CredentialTypesServiceLogs[F]
      val mid = logs
      mid attach new CredentialTypesServiceImpl[F](credentialTypeRepository)
    }

  def unsafe[F[_]: BracketThrow, R[_]: Comonad](
      credentialTypeRepository: CredentialTypeRepository[F],
      logs: Logs[R, F]
  ): CredentialTypesService[F] =
    CredentialTypesService(credentialTypeRepository, logs).extract

  def makeResource[F[_]: BracketThrow, R[_]: Monad](
      credentialTypeRepository: CredentialTypeRepository[F],
      logs: Logs[R, F]
  ): Resource[R, CredentialTypesService[F]] =
    Resource.eval(CredentialTypesService(credentialTypeRepository, logs))
}

private final class CredentialTypesServiceImpl[F[_]](
    credentialTypeRepository: CredentialTypeRepository[F]
) extends CredentialTypesService[F] {
  override def getCredentialTypes(
      institution: ParticipantId
  ): F[List[CredentialType]] =
    credentialTypeRepository.findByInstitution(institution)

  override def getCredentialType(
      institution: ParticipantId,
      getCredentialType: GetCredentialType
  ): F[Option[CredentialTypeWithRequiredFields]] =
    credentialTypeRepository.find(
      institution,
      getCredentialType.credentialTypeId
    )

  override def createCredentialType(
      participantId: ParticipantId,
      createCredentialType: CreateCredentialType
  ): F[Either[ManagementConsoleError, CredentialTypeWithRequiredFields]] =
    credentialTypeRepository.create(participantId, createCredentialType)

  override def updateCredentialType(
      institutionId: ParticipantId,
      updateCredentialType: UpdateCredentialType
  ): F[Either[ManagementConsoleError, Unit]] =
    credentialTypeRepository.update(updateCredentialType, institutionId)

  override def markAsReady(
      participantId: ParticipantId,
      markAsReadyCredentialType: MarkAsReadyCredentialType
  ): F[Either[ManagementConsoleError, Unit]] =
    credentialTypeRepository.markAsReady(
      markAsReadyCredentialType.credentialTypeId,
      participantId
    )

  override def markAsArchived(
      participantId: ParticipantId,
      credentialTypeId: CredentialTypeId
  ): F[Either[ManagementConsoleError, Unit]] =
    credentialTypeRepository.markAsArchived(credentialTypeId, participantId)
}

private final class CredentialTypesServiceLogs[
    F[_]: ServiceLogging[*[_], CredentialTypesService[F]]: MonadThrow
] extends CredentialTypesService[Mid[F, *]] {
  override def getCredentialTypes(
      institution: ParticipantId
  ): Mid[F, List[CredentialType]] =
    in =>
      info"getting credential types $institution" *> in
        .flatTap(_ => info"getting credential types successfully done")
        .onError(
          errorCause"encountered an error while getting credential types" (_)
        )

  override def getCredentialType(
      institution: ParticipantId,
      getCredentialType: GetCredentialType
  ): Mid[F, Option[CredentialTypeWithRequiredFields]] =
    in =>
      info"getting credential type $institution" *> in
        .flatTap(_ => info"getting credential type successfully done")
        .onError(
          errorCause"encountered an error while getting credential type" (_)
        )

  override def createCredentialType(
      participantId: ParticipantId,
      createCredentialType: CreateCredentialType
  ): Mid[F, Either[ManagementConsoleError, CredentialTypeWithRequiredFields]] =
    in =>
      info"creating credential type $participantId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while creating credential type $e",
            _ => info"creating credential type - successfully done"
          )
        )
        .onError(
          errorCause"encountered an error while creating credential type" (_)
        )

  override def updateCredentialType(
      institutionId: ParticipantId,
      updateCredentialType: UpdateCredentialType
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

  override def markAsReady(
      participantId: ParticipantId,
      markAsReadyCredentialType: MarkAsReadyCredentialType
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"marking credential type as ready $participantId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while marking credential type as ready $e",
            _ => info"marking credential type as ready - successfully done"
          )
        )
        .onError(
          errorCause"encountered an error while marking credential type as ready" (
            _
          )
        )

  override def markAsArchived(
      participantId: ParticipantId,
      credentialTypeId: CredentialTypeId
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"marking credential type as archived $participantId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while marking credential type as archived $e",
            _ => info"marking credential type as archived - successfully done"
          )
        )
        .onError(
          errorCause"encountered an error while marking credential type as archived" (
            _
          )
        )
}
