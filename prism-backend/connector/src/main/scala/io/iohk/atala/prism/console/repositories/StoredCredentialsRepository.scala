package io.iohk.atala.prism.console.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.console.models.{Contact, CredentialExternalId, Institution, ReceivedSignedCredential}
import io.iohk.atala.prism.console.repositories.daos.ReceivedCredentialsDAO
import io.iohk.atala.prism.console.repositories.daos.ReceivedCredentialsDAO.StoredReceivedSignedCredentialData
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class StoredCredentialsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def getCredentialsFor(
      verifierId: Institution.Id,
      maybeContactId: Option[Contact.Id]
  ): FutureEither[Nothing, Seq[ReceivedSignedCredential]] = {
    ReceivedCredentialsDAO
      .getReceivedCredentialsFor(verifierId, maybeContactId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def storeCredential(data: StoredReceivedSignedCredentialData): FutureEither[Nothing, Unit] = {
    ReceivedCredentialsDAO
      .storeReceivedSignedCredential(data)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getLatestCredentialExternalId(verifierId: Institution.Id): FutureEither[Nothing, Option[CredentialExternalId]] = {
    ReceivedCredentialsDAO
      .getLatestCredentialExternalId(verifierId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
