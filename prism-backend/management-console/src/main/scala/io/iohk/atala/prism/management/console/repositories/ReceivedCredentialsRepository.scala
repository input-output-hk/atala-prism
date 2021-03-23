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
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

class ReceivedCredentialsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def getCredentialsFor(
      verifierId: ParticipantId,
      contactId: Contact.Id
  ): FutureEither[Nothing, Seq[ReceivedSignedCredential]] = {
    ReceivedCredentialsDAO
      .getReceivedCredentialsFor(verifierId, contactId)
      .logSQLErrors(s"getting credentials, verifier id - $verifierId", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def createReceivedCredential(data: ReceivedSignedCredentialData): FutureEither[Nothing, Unit] = {
    ReceivedCredentialsDAO
      .insertSignedCredential(data)
      .logSQLErrors("creating received credentials", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getLatestCredentialExternalId(verifierId: ParticipantId): FutureEither[Nothing, Option[CredentialExternalId]] = {
    ReceivedCredentialsDAO
      .getLatestCredentialExternalId(verifierId)
      .logSQLErrors(s"getting latest credential external id, verifier id -  $verifierId", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
