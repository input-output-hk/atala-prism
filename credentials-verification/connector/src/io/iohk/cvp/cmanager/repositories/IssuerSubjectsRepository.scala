package io.iohk.cvp.cmanager.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.cvp.cmanager.models.requests.CreateSubject
import io.iohk.cvp.cmanager.models.{Issuer, Subject}
import io.iohk.cvp.cmanager.repositories.daos.{IssuerGroupsDAO, IssuerSubjectsDAO}
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class IssuerSubjectsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(data: CreateSubject): FutureEither[Nothing, Subject] = {
    val query = for {
      groupMaybe <- IssuerGroupsDAO.find(data.issuerId, data.groupName)
      group = groupMaybe.getOrElse(throw new RuntimeException("Group not found"))
      student <- IssuerSubjectsDAO.create(data, group.id)
    } yield student

    query
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def find(issuerId: Issuer.Id, subjectId: Subject.Id): FutureEither[Nothing, Option[Subject]] = {
    IssuerSubjectsDAO
      .findSubject(issuerId, subjectId)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }
}
