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
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models.{CredentialIssuance, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.CredentialIssuancesRepository
import io.iohk.atala.prism.management.console.repositories.CredentialIssuancesRepository.{CreateCredentialIssuance, _}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.logging._

@derive(applyK)
trait CredentialIssuanceService[F[_]] {
  def createCredentialIssuance(
      participantId: ParticipantId,
      createCredentialIssuance: CreateCredentialIssuance
  ): F[Either[ManagementConsoleError, CredentialIssuance.Id]]

  def getCredentialIssuance(
      participantId: ParticipantId,
      getCredentialIssuance: GetCredentialIssuance
  ): F[CredentialIssuance]

  def createGenericCredentialBulk(
      participantId: ParticipantId,
      createCredentialBulk: CreateCredentialBulk
  ): F[Either[ManagementConsoleError, CredentialIssuance.Id]]
}

object CredentialIssuanceService {
  def apply[F[_]: MonadThrow, R[_]: Functor](
      credentialIssuancesRepository: CredentialIssuancesRepository[F],
      logs: Logs[R, F]
  ): R[CredentialIssuanceService[F]] =
    for {
      serviceLogs <- logs.service[CredentialIssuanceService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, CredentialIssuanceService[F]] = serviceLogs
      val logs: CredentialIssuanceService[Mid[F, *]] =
        new CredentialIssuanceServiceLogs[F]
      val mid = logs
      mid attach new CredentialIssuanceServiceImpl[F](
        credentialIssuancesRepository
      )
    }

  def unsafe[F[_]: BracketThrow, R[_]: Comonad](
      credentialIssuancesRepository: CredentialIssuancesRepository[F],
      logs: Logs[R, F]
  ): CredentialIssuanceService[F] =
    CredentialIssuanceService(credentialIssuancesRepository, logs).extract

  def makeResource[F[_]: BracketThrow, R[_]: Monad](
      credentialIssuancesRepository: CredentialIssuancesRepository[F],
      logs: Logs[R, F]
  ): Resource[R, CredentialIssuanceService[F]] =
    Resource.eval(
      CredentialIssuanceService(credentialIssuancesRepository, logs)
    )
}

private final class CredentialIssuanceServiceImpl[F[_]](
    credentialIssuancesRepository: CredentialIssuancesRepository[F]
) extends CredentialIssuanceService[F] {
  override def createCredentialIssuance(
      participantId: ParticipantId,
      createCredentialIssuance: CreateCredentialIssuance
  ): F[Either[ManagementConsoleError, CredentialIssuance.Id]] =
    credentialIssuancesRepository.create(
      participantId,
      createCredentialIssuance
    )

  override def getCredentialIssuance(
      participantId: ParticipantId,
      getCredentialIssuance: GetCredentialIssuance
  ): F[CredentialIssuance] =
    credentialIssuancesRepository
      .get(getCredentialIssuance.credentialIssuanceId, participantId)

  override def createGenericCredentialBulk(
      participantId: ParticipantId,
      createCredentialBulk: CreateCredentialBulk
  ): F[Either[ManagementConsoleError, CredentialIssuance.Id]] =
    credentialIssuancesRepository.createBulk(
      participantId,
      createCredentialBulk.credentialsType,
      createCredentialBulk.issuanceName,
      createCredentialBulk.drafts
    )
}

private final class CredentialIssuanceServiceLogs[
    F[_]: ServiceLogging[*[_], CredentialIssuanceService[F]]: MonadThrow
] extends CredentialIssuanceService[Mid[F, *]] {
  override def createCredentialIssuance(
      participantId: ParticipantId,
      createCredentialIssuance: CreateCredentialIssuance
  ): Mid[F, Either[ManagementConsoleError, CredentialIssuance.Id]] =
    in =>
      info"creating credential issuance $participantId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while creating credential issuance $e",
            _ => info"creating credential issuance - successfully done"
          )
        )
        .onError(
          errorCause"encountered an error while creating credential issuance" (
            _
          )
        )

  override def getCredentialIssuance(
      participantId: ParticipantId,
      getCredentialIssuance: GetCredentialIssuance
  ): Mid[F, CredentialIssuance] =
    in =>
      info"getting credential $participantId" *> in
        .flatTap(_ => info"getting credential - successfully done")
        .onError(errorCause"encountered an error while getting credential" (_))

  override def createGenericCredentialBulk(
      participantId: ParticipantId,
      createCredentialBulk: CreateCredentialBulk
  ): Mid[F, Either[ManagementConsoleError, CredentialIssuance.Id]] =
    in =>
      info"creating bulk credential issuance $participantId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while creating bulk credential issuance  $e",
            _ => info"creating bulk credential issuance - successfully done"
          )
        )
        .onError(
          errorCause"encountered an error while creating bulk credential issuance" (
            _
          )
        )
}
