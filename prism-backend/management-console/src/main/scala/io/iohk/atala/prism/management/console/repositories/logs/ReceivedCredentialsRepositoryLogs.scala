package io.iohk.atala.prism.management.console.repositories.logs

import cats.effect.BracketThrow
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.ReceivedCredentialsRepository
import io.iohk.atala.prism.management.console.repositories.daos.ReceivedCredentialsDAO.ReceivedSignedCredentialData
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

private[repositories] final class ReceivedCredentialsRepositoryLogs[F[
    _
]: ServiceLogging[
  *[_],
  ReceivedCredentialsRepository[F]
]: BracketThrow]
    extends ReceivedCredentialsRepository[Mid[F, *]] {

  override def getCredentialsFor(
      verifierId: ParticipantId,
      contactId: Option[Contact.Id]
  ): Mid[F, List[ReceivedSignedCredential]] =
    in =>
      info"getting credentials for $verifierId $contactId" *> in
        .flatTap(list => info"getting credentials for - got ${list.size} entities")
        .onError(
          errorCause"encountered an error while getting credentials for" (_)
        )

  override def createReceivedCredential(
      data: ReceivedSignedCredentialData
  ): Mid[F, Unit] =
    in =>
      info"creating received credential ${data.contactId} ${data.credentialExternalId}" *> in
        .flatTap(_ => info"creating received credential - successfully done")
        .onError(
          errorCause"encountered an error while creating received credential" (
            _
          )
        )

  override def getLatestCredentialExternalId(
      verifierId: ParticipantId
  ): Mid[F, Option[CredentialExternalId]] =
    in =>
      info"getting credential external id $verifierId" *> in
        .flatTap(
          _.fold(info"getting credential external id - got nothing")(externalId =>
            info"getting credential external id - $externalId"
          )
        )
        .onError(
          errorCause"encountered an error while getting credential external id" (
            _
          )
        )
}
