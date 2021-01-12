package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie.implicits._
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CreateGenericCredential,
  GenericCredential,
  ParticipantId,
  PublishCredential
}
import io.iohk.atala.prism.management.console.repositories.daos.CredentialsDAO
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
      issuedBy: ParticipantId,
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

  def getBy(issuedBy: ParticipantId, subjectId: Contact.Id): FutureEither[Nothing, List[GenericCredential]] = {
    CredentialsDAO
      .getBy(issuedBy, subjectId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def storePublicationData(issuerId: ParticipantId, credentialData: PublishCredential): FutureEither[Nothing, Int] = {
    CredentialsDAO
      .storePublicationData(issuerId, credentialData)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def markAsShared(issuerId: ParticipantId, credentialId: GenericCredential.Id): FutureEither[Nothing, Unit] = {
    CredentialsDAO
      .markAsShared(issuerId, credentialId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
