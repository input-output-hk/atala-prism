package io.iohk.atala.prism.console.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.console.models._
import io.iohk.atala.prism.console.repositories.daos.CredentialsDAO
import io.iohk.atala.prism.models.TransactionId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

class CredentialsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def create(data: CreateGenericCredential): FutureEither[Nothing, GenericCredential] = {
    CredentialsDAO
      .create(data)
      .logSQLErrors("creating credentials", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getBy(credentialId: GenericCredential.Id): FutureEither[Nothing, Option[GenericCredential]] = {
    CredentialsDAO
      .getBy(credentialId)
      .logSQLErrors(s"getting, credential id - $credentialId ", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getBy(
      issuedBy: Institution.Id,
      limit: Int,
      lastSeenCredential: Option[GenericCredential.Id]
  ): FutureEither[Nothing, List[GenericCredential]] = {
    CredentialsDAO
      .getBy(issuedBy, limit, lastSeenCredential)
      .logSQLErrors(s"getting, issued by - $issuedBy ", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getBy(
      issuedBy: Institution.Id,
      limit: Int,
      offset: Int
  ): FutureEither[Nothing, List[GenericCredential]] = {
    CredentialsDAO
      .getBy(issuedBy, limit, offset)
      .logSQLErrors(s"getting, issued by - $issuedBy ", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getBy(issuedBy: Institution.Id, subjectId: Contact.Id): FutureEither[Nothing, List[GenericCredential]] = {
    CredentialsDAO
      .getBy(issuedBy, subjectId)
      .logSQLErrors(s"getting, subject id - $subjectId ", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def storeCredentialPublicationData(
      issuerId: Institution.Id,
      credentialData: CredentialPublicationData
  ): FutureEither[Nothing, Int] = {
    CredentialsDAO
      .storeCredentialPublicationData(issuerId, credentialData)
      .logSQLErrors(s"storing credential publication data, issuer id - $issuerId", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def storeBatchData(batchData: StoreBatchData): FutureEither[Nothing, Int] = {
    CredentialsDAO
      .storeBatchData(batchData)
      .logSQLErrors("storing credentials by batch", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def storeRevocationData(
      institutionId: Institution.Id,
      credentialId: GenericCredential.Id,
      transactionId: TransactionId
  ): FutureEither[Nothing, Unit] = {
    CredentialsDAO
      .revokeCredential(institutionId, credentialId, transactionId)
      .logSQLErrors(s"storing revocation data, institution id - $institutionId", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def markAsShared(issuerId: Institution.Id, credentialId: GenericCredential.Id): FutureEither[Nothing, Unit] = {
    CredentialsDAO
      .markAsShared(issuerId, credentialId)
      .logSQLErrors(s"marking as shared, credential id - $credentialId", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
