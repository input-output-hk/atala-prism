package io.iohk.atala.prism.console.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.console.models.{Contact, CredentialExternalId, Institution, ReceivedSignedCredential}
import io.iohk.atala.prism.console.repositories.daos.ReceivedCredentialsDAO
import io.iohk.atala.prism.console.repositories.daos.ReceivedCredentialsDAO.StoredReceivedSignedCredentialData
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

class StoredCredentialsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def getCredentialsFor(
      verifierId: Institution.Id,
      maybeContactId: Option[Contact.Id]
  ): FutureEither[Nothing, Seq[ReceivedSignedCredential]] = {
    ReceivedCredentialsDAO
      .getReceivedCredentialsFor(verifierId, maybeContactId)
      .logSQLErrors(s"getting received credentials, verifier id - $verifierId", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def storeCredential(data: StoredReceivedSignedCredentialData): FutureEither[Nothing, Unit] = {
    ReceivedCredentialsDAO
      .storeReceivedSignedCredential(data)
      .logSQLErrors("storing received credentials", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getLatestCredentialExternalId(verifierId: Institution.Id): FutureEither[Nothing, Option[CredentialExternalId]] = {
    ReceivedCredentialsDAO
      .getLatestCredentialExternalId(verifierId)
      .logSQLErrors(s"getting latest credential, id verifier id - $verifierId", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
