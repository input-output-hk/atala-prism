package io.iohk.cvp.cmanager.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.cvp.cmanager.models.requests.CreateCredential
import io.iohk.cvp.cmanager.models.{Credential, Issuer, Student}
import io.iohk.cvp.cmanager.repositories.daos.CredentialsDAO
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class CredentialsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  def create(data: CreateCredential): FutureEither[Nothing, Credential] = {
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
      lastSeenCredential: Option[Credential.Id]
  ): FutureEither[Nothing, List[Credential]] = {
    CredentialsDAO
      .getBy(issuedBy, limit, lastSeenCredential)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getBy(issuedBy: Issuer.Id, studentId: Student.Id): FutureEither[Nothing, List[Credential]] = {
    CredentialsDAO
      .getBy(issuedBy, studentId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
