package io.iohk.cvp.cmanager.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.cvp.cmanager.models.requests.{CreateGenericCredential, CreateUniversityCredential}
import io.iohk.cvp.cmanager.models.{GenericCredential, Issuer, Student, Subject, UniversityCredential}
import io.iohk.cvp.cmanager.repositories.daos.CredentialsDAO
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

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
      issuedBy: Issuer.Id,
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
      issuedBy: Issuer.Id,
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
      issuedBy: Issuer.Id,
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

  def getBy(issuedBy: Issuer.Id, subjectId: Subject.Id): FutureEither[Nothing, List[GenericCredential]] = {
    CredentialsDAO
      .getBy(issuedBy, subjectId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
