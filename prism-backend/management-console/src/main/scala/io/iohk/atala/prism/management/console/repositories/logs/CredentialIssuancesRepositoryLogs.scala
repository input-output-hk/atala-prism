package io.iohk.atala.prism.management.console.repositories.logs

import cats.effect.BracketThrow
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models.{CredentialIssuance, CredentialTypeId, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.CredentialIssuancesRepository
import io.iohk.atala.prism.management.console.repositories.CredentialIssuancesRepository.{
  CreateCredentialBulk,
  CreateCredentialIssuance
}
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

private[repositories] final class CredentialIssuancesRepositoryLogs[F[
    _
]: BracketThrow: ServiceLogging[*[
  _
], CredentialIssuancesRepository[
  F
]]] extends CredentialIssuancesRepository[Mid[F, *]] {

  override def create(
      participantId: ParticipantId,
      createCredentialIssuance: CreateCredentialIssuance
  ): Mid[F, Either[ManagementConsoleError, CredentialIssuance.Id]] =
    in =>
      info"creating credential issuance $participantId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while creating credential issuance $e",
            r => info"creating credential issuance - successfully done $r"
          )
        )
        .onError(
          errorCause"encountered an error while creating credential issuance" (
            _
          )
        )

  override def createBulk(
      participantId: ParticipantId,
      credentialsType: CredentialTypeId,
      issuanceName: String,
      drafts: List[CreateCredentialBulk.Draft]
  ): Mid[F, Either[ManagementConsoleError, CredentialIssuance.Id]] =
    in =>
      info"creating bulk credential issuance $participantId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while creating bulk credential issuance  $e",
            r => info"creating bulk credential issuance - successfully done $r"
          )
        )
        .onError(
          errorCause"encountered an error while creating bulk credential issuance" (
            _
          )
        )

  override def get(
      credentialIssuanceId: CredentialIssuance.Id,
      institutionId: ParticipantId
  ): Mid[F, CredentialIssuance] =
    in =>
      info"getting credential $credentialIssuanceId $institutionId" *> in
        .flatTap(_ => info"getting credential - successfully done")
        .onError(errorCause"encountered an error while getting credential" (_))
}
