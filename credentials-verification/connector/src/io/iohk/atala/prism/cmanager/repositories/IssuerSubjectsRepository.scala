package io.iohk.atala.prism.cmanager.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.connector.repositories.daos.ConnectionTokensDAO
import io.iohk.atala.prism.cmanager.models.requests.CreateSubject
import io.iohk.atala.prism.cmanager.models.{Issuer, IssuerGroup, Subject}
import io.iohk.atala.prism.cmanager.repositories.daos.{IssuerGroupsDAO, IssuerSubjectsDAO}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class IssuerSubjectsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(data: CreateSubject, maybeGroupName: Option[IssuerGroup.Name]): FutureEither[Nothing, Subject] = {
    val query = maybeGroupName match {
      case None => // if we do not request the subject to be added to a group
        IssuerSubjectsDAO.create(data)
      case Some(groupName) => // if we are requesting to add a subject to a group
        for {
          subject <- IssuerSubjectsDAO.create(data)
          groupMaybe <- IssuerGroupsDAO.find(data.issuerId, groupName)
          group = groupMaybe.getOrElse(throw new RuntimeException(s"Group $groupName does not exist"))
          _ <- IssuerGroupsDAO.addSubject(group.id, subject.id)
        } yield subject
    }

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

  def getBy(
      issuer: Issuer.Id,
      limit: Int,
      lastSeenSubject: Option[Subject.Id],
      groupName: Option[IssuerGroup.Name]
  ): FutureEither[Nothing, List[Subject]] = {
    IssuerSubjectsDAO
      .getBy(issuer, limit, lastSeenSubject, groupName)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def generateToken(issuerId: Issuer.Id, subjectId: Subject.Id): FutureEither[Nothing, TokenString] = {
    val token = TokenString.random()

    val tx = for {
      _ <- ConnectionTokensDAO.insert(ParticipantId(issuerId.value), token)
      _ <- IssuerSubjectsDAO.update(
        issuerId,
        IssuerSubjectsDAO.UpdateSubjectRequest.ConnectionTokenGenerated(subjectId, token)
      )
    } yield ()

    tx.transact(xa)
      .unsafeToFuture()
      .map(_ => Right(token))
      .toFutureEither
  }

}
