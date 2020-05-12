package io.iohk.cvp.cmanager.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.connector.model.TokenString
import io.iohk.connector.repositories.daos.ConnectionTokensDAO
import io.iohk.cvp.cmanager.models.requests.CreateStudent
import io.iohk.cvp.cmanager.models.{Issuer, IssuerGroup, Student}
import io.iohk.cvp.cmanager.repositories.daos.{IssuerGroupsDAO, IssuerSubjectsDAO}
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class StudentsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(data: CreateStudent): FutureEither[Nothing, Student] = {
    val query = for {
      groupMaybe <- IssuerGroupsDAO.find(data.issuer, data.groupName)
      group = groupMaybe.getOrElse(throw new RuntimeException("Group not found"))
      student <- IssuerSubjectsDAO.create(data, group.id)
    } yield student

    query
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getBy(
      issuer: Issuer.Id,
      limit: Int,
      lastSeenStudent: Option[Student.Id],
      groupName: Option[IssuerGroup.Name]
  ): FutureEither[Nothing, List[Student]] = {
    IssuerSubjectsDAO
      .getBy(issuer, limit, lastSeenStudent, groupName)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def find(issuerId: Issuer.Id, studentId: Student.Id): FutureEither[Nothing, Option[Student]] = {
    IssuerSubjectsDAO
      .find(issuerId, studentId)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }

  def generateToken(issuerId: Issuer.Id, studentId: Student.Id): FutureEither[Nothing, TokenString] = {
    val token = TokenString.random()

    val tx = for {
      _ <- ConnectionTokensDAO.insert(toParticipantId(issuerId), token)
      _ <- IssuerSubjectsDAO.update(
        issuerId,
        IssuerSubjectsDAO.UpdateSubjectRequest.ConnectionTokenGenerated(studentId, token)
      )
    } yield ()

    tx.transact(xa)
      .unsafeToFuture()
      .map(_ => Right(token))
      .toFutureEither
  }

  private def toParticipantId(issuer: Issuer.Id): ParticipantId = {
    ParticipantId(issuer.value)
  }
}
