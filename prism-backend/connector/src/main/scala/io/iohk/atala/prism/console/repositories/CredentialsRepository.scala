package io.iohk.atala.prism.console.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.console.models._
import io.iohk.atala.prism.console.repositories.daos.CredentialsDAO
import io.iohk.atala.prism.models.TransactionInfo
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class CredentialsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  def create(data: CreateGenericCredential): FutureEither[Nothing, GenericCredential] = {
    CredentialsDAO
      .create(data)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getBy(credentialId: GenericCredential.Id): FutureEither[Nothing, Option[GenericCredential]] = {
    CredentialsDAO
      .getBy(credentialId)
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
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getBy(issuedBy: Institution.Id, subjectId: Contact.Id): FutureEither[Nothing, List[GenericCredential]] = {
    CredentialsDAO
      .getBy(issuedBy, subjectId)
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
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def storeBatchData(batchData: StoreBatchData): FutureEither[Nothing, Int] = {
    CredentialsDAO
      .storeBatchData(batchData)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def markAsShared(issuerId: Institution.Id, credentialId: GenericCredential.Id): FutureEither[Nothing, Unit] = {
    CredentialsDAO
      .markAsShared(issuerId, credentialId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  // TODO: Should be removed when we get a node RPC to get this data
  def getTransactionInfo(encodedSignedCredential: String): FutureEither[Nothing, Option[TransactionInfo]] = {
    CredentialsDAO
      .getTransactionInfo(encodedSignedCredential)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
