package io.iohk.atala.prism.management.console.services

import cats.{Comonad, Functor, Monad}
import cats.effect.{MonadThrow, Resource}
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.comonad._
import cats.syntax.flatMap._
import cats.syntax.functor._
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.integrations.ParticipantsIntegrationService
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.StatisticsRepository
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.logging._

@derive(applyK)
trait ConsoleService[F[_]] {
  def getStatistics(
      participantId: ParticipantId,
      getStatistics: GetStatistics
  ): F[Statistics]

  def registerDID(
      registerDID: RegisterDID
  ): F[Either[ManagementConsoleError, Unit]]

  def getCurrentUser(
      participantId: ParticipantId
  ): F[Either[ManagementConsoleError, ParticipantInfo]]

  def updateParticipantProfile(
      participantId: ParticipantId,
      participantProfile: UpdateParticipantProfile
  ): F[Unit]
}

object ConsoleService {
  def apply[F[_]: MonadThrow, R[_]: Functor](
      participantsIntegrationService: ParticipantsIntegrationService[F],
      statisticsRepository: StatisticsRepository[F],
      logs: Logs[R, F]
  ): R[ConsoleService[F]] =
    for {
      serviceLogs <- logs.service[ConsoleService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, ConsoleService[F]] =
        serviceLogs
      val logs: ConsoleService[Mid[F, *]] = new ConsoleServiceLogs[F]
      val mid = logs
      mid attach new ConsoleServiceImpl[F](
        participantsIntegrationService,
        statisticsRepository
      )
    }

  def unsafe[F[_]: MonadThrow, R[_]: Comonad](
      participantsIntegrationService: ParticipantsIntegrationService[F],
      statisticsRepository: StatisticsRepository[F],
      logs: Logs[R, F]
  ): ConsoleService[F] = ConsoleService(
    participantsIntegrationService,
    statisticsRepository,
    logs
  ).extract

  def makeResource[F[_]: MonadThrow, R[_]: Monad](
      participantsIntegrationService: ParticipantsIntegrationService[F],
      statisticsRepository: StatisticsRepository[F],
      logs: Logs[R, F]
  ): Resource[R, ConsoleService[F]] =
    Resource.eval(
      ConsoleService(participantsIntegrationService, statisticsRepository, logs)
    )
}

private final class ConsoleServiceImpl[F[_]](
    participantsIntegrationService: ParticipantsIntegrationService[F],
    statisticsRepository: StatisticsRepository[F]
) extends ConsoleService[F] {

  override def getStatistics(
      participantId: ParticipantId,
      getStatistics: GetStatistics
  ): F[Statistics] =
    statisticsRepository.query(participantId, getStatistics.timeInterval)

  override def registerDID(
      registerDID: RegisterDID
  ): F[Either[ManagementConsoleError, Unit]] =
    participantsIntegrationService.register(registerDID)

  override def getCurrentUser(
      participantId: ParticipantId
  ): F[Either[ManagementConsoleError, ParticipantInfo]] =
    participantsIntegrationService.getDetails(participantId)

  override def updateParticipantProfile(
      participantId: ParticipantId,
      participantProfile: UpdateParticipantProfile
  ): F[Unit] =
    participantsIntegrationService.update(participantId, participantProfile)
}

private final class ConsoleServiceLogs[
    F[_]: ServiceLogging[*[_], ConsoleService[F]]: MonadThrow
] extends ConsoleService[Mid[F, *]] {
  override def getStatistics(
      participantId: ParticipantId,
      getStatistics: GetStatistics
  ): Mid[F, Statistics] =
    in =>
      info"getting statistics $participantId" *> in
        .flatTap(_ => info"getting statistics - successfully done")
        .onError(errorCause"encountered an error while getting statistics" (_))

  override def registerDID(
      registerDID: RegisterDID
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"registering DID ${registerDID.did.asCanonical().getSuffix}" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while registering DID $er",
            _ => info"registering DID - successfully done"
          )
        )
        .onError(errorCause"encountered an error while registering DID" (_))

  override def getCurrentUser(
      participantId: ParticipantId
  ): Mid[F, Either[ManagementConsoleError, ParticipantInfo]] =
    in =>
      info"getting current user $participantId" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while getting current user $er",
            _ => info"getting current user - successfully done"
          )
        )
        .onError(
          errorCause"encountered an error while getting current user" (_)
        )

  override def updateParticipantProfile(
      participantId: ParticipantId,
      participantProfile: UpdateParticipantProfile
  ): Mid[F, Unit] =
    in =>
      info"updating participant profile $participantId" *> in
        .flatTap(_ => info"updating participant profile - successfully done")
        .onError(
          errorCause"encountered an error while updating participant profile" (
            _
          )
        )
}
