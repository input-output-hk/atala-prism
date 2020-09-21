package io.iohk.atala.prism.cmanager.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.connector.repositories.daos.ConnectionTokensDAO
import io.iohk.atala.prism.cmanager.models.requests.CreateStudent
import io.iohk.atala.prism.cmanager.models.{IssuerGroup, Student}
import io.iohk.atala.prism.cmanager.repositories.daos.{IssuerGroupsDAO, IssuerSubjectsDAO}
import io.iohk.atala.prism.console.models.{Contact, Institution}
import io.iohk.atala.prism.console.repositories.daos.ContactsDAO
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
          groupMaybe <- IssuerGroupsDAO.find(Institution.Id(data.issuer.value), groupName)
          group = groupMaybe.getOrElse(throw new RuntimeException(s"Group $groupName does not exist"))
          _ <- IssuerGroupsDAO.addContact(group.id, Contact.Id(student.id.value))
        } yield student
    }

    query
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getBy(
      issuer: Institution.Id,
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

  def find(issuerId: Institution.Id, studentId: Student.Id): FutureEither[Nothing, Option[Student]] = {
    IssuerSubjectsDAO
      .findStudent(issuerId, studentId)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }

  def generateToken(issuerId: Institution.Id, studentId: Student.Id): FutureEither[Nothing, TokenString] = {
    val token = TokenString.random()

    val tx = for {
      _ <- ConnectionTokensDAO.insert(ParticipantId(issuerId.value), token)
      _ <- ContactsDAO.setConnectionToken(
        issuerId,
        Contact.Id(studentId.value),
        token
      )
    } yield ()

    tx.transact(xa)
      .unsafeToFuture()
      .map(_ => Right(token))
      .toFutureEither
  }
}
