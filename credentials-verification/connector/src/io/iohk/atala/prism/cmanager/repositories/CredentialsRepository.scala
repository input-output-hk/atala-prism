package io.iohk.atala.prism.cmanager.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.cmanager.models.requests.{
  CreateGenericCredential,
  CreateUniversityCredential,
  PublishCredential
}
import io.iohk.atala.prism.cmanager.models.{GenericCredential, Student, UniversityCredential}
import io.iohk.atala.prism.cmanager.repositories.daos.CredentialsDAO
import io.iohk.atala.prism.console.models.{Contact, Institution}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class CredentialsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  def createUniversityCredential(data: CreateUniversityCredential): FutureEither[Nothing, UniversityCredential] = {
    CredentialsDAO
      .createUniversityCredential(data)
      .transact(xa)
      .unsafeToFuture
      .map(Right(_))
      .toFutureEither
  }

  def getUniversityCredentialsBy(
      issuedBy: Institution.Id,
      limit: Int,
      lastSeenCredential: Option[UniversityCredential.Id]
  ): FutureEither[Nothing, List[UniversityCredential]] = {
    CredentialsDAO
      .getUniversityCredentialsBy(issuedBy, limit, lastSeenCredential)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getUniversityCredentialsBy(
      issuedBy: Institution.Id,
      studentId: Student.Id
  ): FutureEither[Nothing, List[UniversityCredential]] = {
    CredentialsDAO
      .getUniversityCredentialsBy(issuedBy, studentId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  // Generic versions
  def create(data: CreateGenericCredential): FutureEither[Nothing, GenericCredential] = {
    CredentialsDAO
      .create(data)
      .transact(xa)
      .unsafeToFuture
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

  def storePublicationData(issuerId: Institution.Id, credentialData: PublishCredential): FutureEither[Nothing, Int] = {
    CredentialsDAO
      .storePublicationData(issuerId, credentialData)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
