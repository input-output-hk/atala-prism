package io.iohk.cvp.cmanager.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.connector.model.TokenString
import io.iohk.connector.repositories.daos.ConnectionTokensDAO
import io.iohk.cvp.cmanager.models.requests.CreateSubject
import io.iohk.cvp.cmanager.models.{Issuer, IssuerGroup, Subject}
import io.iohk.cvp.cmanager.repositories.daos.{IssuerGroupsDAO, IssuerSubjectsDAO}
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class IssuerSubjectsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(data: CreateSubject): FutureEither[Nothing, Subject] = {
    val query = for {
      groupMaybe <- IssuerGroupsDAO.find(data.issuerId, data.groupName)
      group = groupMaybe.getOrElse(throw new RuntimeException("Group not found"))
      subject <- IssuerSubjectsDAO.create(data, group.id)
    } yield subject

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
