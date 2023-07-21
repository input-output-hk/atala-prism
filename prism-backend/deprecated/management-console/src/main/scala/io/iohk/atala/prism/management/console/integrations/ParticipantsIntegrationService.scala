package io.iohk.atala.prism.management.console.integrations

import cats.{Comonad, Functor, Monad}
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.comonad._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.effect.Resource
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.management.console.errors
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.ParticipantsRepository
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.logging._
import cats.MonadThrow
import cats.effect.MonadCancelThrow

@derive(applyK)
trait ParticipantsIntegrationService[F[_]] {
  def register(request: RegisterDID): F[Either[ManagementConsoleError, Unit]]
  def getDetails(
      participantId: ParticipantId
  ): F[Either[errors.ManagementConsoleError, ParticipantInfo]]
  def update(
      participantId: ParticipantId,
      participantProfile: UpdateParticipantProfile
  ): F[Unit]
}

object ParticipantsIntegrationService {
  def apply[F[_]: MonadCancelThrow, R[_]: Functor](
      participantsRepository: ParticipantsRepository[F],
      logs: Logs[R, F]
  ): R[ParticipantsIntegrationService[F]] =
    for {
      serviceLogs <- logs.service[ParticipantsIntegrationService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, ParticipantsIntegrationService[F]] = serviceLogs
      val logs: ParticipantsIntegrationService[Mid[F, *]] =
        new ParticipantsIntegrationServiceLogs[F]
      val mid = logs
      mid attach new ParticipantsIntegrationServiceImpl[F](
        participantsRepository
      )
    }

  def unsafe[F[_]: MonadCancelThrow, R[_]: Comonad](
      participantsRepository: ParticipantsRepository[F],
      logs: Logs[R, F]
  ): ParticipantsIntegrationService[F] =
    ParticipantsIntegrationService(participantsRepository, logs).extract

  def makeResource[F[_]: MonadCancelThrow, R[_]: Monad](
      participantsRepository: ParticipantsRepository[F],
      logs: Logs[R, F]
  ): Resource[R, ParticipantsIntegrationService[F]] =
    Resource.eval(ParticipantsIntegrationService(participantsRepository, logs))
}

private final class ParticipantsIntegrationServiceImpl[F[_]](
    participantsRepository: ParticipantsRepository[F]
) extends ParticipantsIntegrationService[F] {

  def register(
      request: RegisterDID
  ): F[Either[ManagementConsoleError, Unit]] = {
    val createRequest = ParticipantsRepository.CreateParticipantRequest(
      id = ParticipantId.random(),
      name = request.name,
      did = request.did,
      logo = request.logo
    )
    participantsRepository.create(createRequest)
  }

  def getDetails(
      participantId: ParticipantId
  ): F[Either[errors.ManagementConsoleError, ParticipantInfo]] =
    participantsRepository.findBy(participantId)

  def update(
      participantId: ParticipantId,
      participantProfile: UpdateParticipantProfile
  ): F[Unit] = {
    val updateRequest = ParticipantsRepository.UpdateParticipantProfileRequest(
      id = participantId,
      participantProfile
    )
    participantsRepository
      .update(updateRequest)
  }
}

private final class ParticipantsIntegrationServiceLogs[F[_]: ServiceLogging[
  *[_],
  ParticipantsIntegrationService[F]
]: MonadThrow]
    extends ParticipantsIntegrationService[Mid[F, *]] {
  override def register(
      request: RegisterDID
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"registering participant ${request.name} ${request.did.asCanonical().getSuffix}" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while registering participant $er",
            _ => info"registering participant - successfully done"
          )
        )
        .onError(
          errorCause"encountered an error while registering participant" (_)
        )

  override def getDetails(
      participantId: ParticipantId
  ): Mid[F, Either[ManagementConsoleError, ParticipantInfo]] =
    in =>
      info"getting details $participantId" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while getting details $er",
            info => info"getting details - successfully done, ${info.id}"
          )
        )
        .onError(errorCause"encountered an error while getting participant" (_))

  override def update(
      participantId: ParticipantId,
      participantProfile: UpdateParticipantProfile
  ): Mid[F, Unit] =
    in =>
      info"updating participant $participantId" *> in
        .flatTap(_ => info"updating participant - successfully done")
        .onError(
          errorCause"encountered an error while updating participant" (_)
        )
}
