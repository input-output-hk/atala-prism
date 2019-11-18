package io.iohk.cvp.cmanager.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.cvp.cmanager.models.requests.CreateStudent
import io.iohk.cvp.cmanager.models.{Issuer, Student}
import io.iohk.cvp.cmanager.repositories.daos.StudentsDAO
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class StudentsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(data: CreateStudent): FutureEither[Nothing, Student] = {
    StudentsDAO
      .create(data)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getBy(
      issuer: Issuer.Id,
      limit: Int,
      lastSeenStudent: Option[Student.Id]
  ): FutureEither[Nothing, List[Student]] = {
    StudentsDAO
      .getBy(issuer, limit, lastSeenStudent)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
