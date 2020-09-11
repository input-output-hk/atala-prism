package io.iohk.atala.prism.cmanager.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.connector.repositories.daos.ConnectionTokensDAO
import io.iohk.atala.prism.cmanager.models.requests.CreateStudent
import io.iohk.atala.prism.cmanager.models.{Issuer, IssuerGroup, Student, Subject}
import io.iohk.atala.prism.cmanager.repositories.daos.{IssuerGroupsDAO, IssuerSubjectsDAO}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class StudentsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def createStudent(data: CreateStudent, maybeGroupName: Option[IssuerGroup.Name]): FutureEither[Nothing, Student] = {
    val query = maybeGroupName match {
      case None => // if we do not request the subject to be added to a group
        IssuerSubjectsDAO.createStudent(data)
      case Some(groupName) => // if we are requesting to add a subject to a group
        for {
          student <- IssuerSubjectsDAO.createStudent(data)
          groupMaybe <- IssuerGroupsDAO.find(data.issuer, groupName)
          group = groupMaybe.getOrElse(throw new RuntimeException(s"Group $groupName does not exist"))
          _ <- IssuerGroupsDAO.addSubject(group.id, Subject.Id(student.id.value))
        } yield student
    }

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
      .getStudentsBy(issuer, limit, lastSeenStudent, groupName)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def find(issuerId: Issuer.Id, studentId: Student.Id): FutureEither[Nothing, Option[Student]] = {
    IssuerSubjectsDAO
      .findStudent(issuerId, studentId)
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
        IssuerSubjectsDAO.UpdateSubjectRequest.ConnectionTokenGenerated(Subject.Id(studentId.value), token)
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
