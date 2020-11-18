package io.iohk.atala.prism.console.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.console.models.{Contact, CredentialExternalId, Institution, StoredSignedCredential}
import io.iohk.atala.prism.console.repositories.daos.StoredCredentialsDAO
import io.iohk.atala.prism.console.repositories.daos.StoredCredentialsDAO.StoredSignedCredentialData
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class StoredCredentialsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def getCredentialsFor(
      verifierId: Institution.Id,
      contactId: Contact.Id
  ): FutureEither[Nothing, Seq[StoredSignedCredential]] = {
    StoredCredentialsDAO
      .getStoredCredentialsFor(verifierId, contactId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def storeCredential(data: StoredSignedCredentialData): FutureEither[Nothing, Unit] = {
    StoredCredentialsDAO
      .storeSignedCredential(data)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getLatestCredentialExternalId(verifierId: Institution.Id): FutureEither[Nothing, Option[CredentialExternalId]] = {
    StoredCredentialsDAO
      .getLatestCredentialExternalId(verifierId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
