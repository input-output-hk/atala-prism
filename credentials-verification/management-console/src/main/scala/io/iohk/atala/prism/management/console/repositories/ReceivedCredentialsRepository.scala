package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CredentialExternalId,
  ParticipantId,
  ReceivedSignedCredential
}
import io.iohk.atala.prism.management.console.repositories.daos.ReceivedCredentialsDAO
import io.iohk.atala.prism.management.console.repositories.daos.ReceivedCredentialsDAO.ReceivedSignedCredentialData
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class ReceivedCredentialsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def getCredentialsFor(
      verifierId: ParticipantId,
      contactId: Contact.Id
  ): FutureEither[Nothing, Seq[ReceivedSignedCredential]] = {
    ReceivedCredentialsDAO
      .getReceivedCredentialsFor(verifierId, contactId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def createReceivedCredential(data: ReceivedSignedCredentialData): FutureEither[Nothing, Unit] = {
    ReceivedCredentialsDAO
      .insertSignedCredential(data)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getLatestCredentialExternalId(verifierId: ParticipantId): FutureEither[Nothing, Option[CredentialExternalId]] = {
    ReceivedCredentialsDAO
      .getLatestCredentialExternalId(verifierId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
