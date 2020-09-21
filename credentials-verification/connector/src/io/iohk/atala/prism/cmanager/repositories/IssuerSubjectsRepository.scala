package io.iohk.atala.prism.cmanager.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.cmanager.models.IssuerGroup
import io.iohk.atala.prism.cmanager.repositories.daos.IssuerGroupsDAO
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.connector.repositories.daos.ConnectionTokensDAO
import io.iohk.atala.prism.console.models.{Contact, CreateContact, Institution}
import io.iohk.atala.prism.console.repositories.daos.ContactsDAO
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class IssuerSubjectsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(contactData: CreateContact, maybeGroupName: Option[IssuerGroup.Name]): FutureEither[Nothing, Contact] = {
    val query = maybeGroupName match {
      case None => // if we do not request the subject to be added to a group
        ContactsDAO.createContact(contactData)
      case Some(groupName) => // if we are requesting to add a subject to a group
        for {
          contact <- ContactsDAO.createContact(contactData)
          groupMaybe <- IssuerGroupsDAO.find(contactData.createdBy, groupName)
          group = groupMaybe.getOrElse(throw new RuntimeException(s"Group $groupName does not exist"))
          _ <- IssuerGroupsDAO.addContact(group.id, contact.id)
        } yield contact
    }

    query
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def find(issuerId: Institution.Id, subjectId: Contact.Id): FutureEither[Nothing, Option[Contact]] = {
    ContactsDAO
      .findContact(issuerId, subjectId)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }

  def find(issuerId: Institution.Id, externalId: Contact.ExternalId): FutureEither[Nothing, Option[Contact]] = {
    ContactsDAO
      .findContact(issuerId, externalId)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }

  def getBy(
      issuer: Institution.Id,
      limit: Int,
      lastSeenSubject: Option[Contact.Id],
      groupName: Option[IssuerGroup.Name]
  ): FutureEither[Nothing, List[Contact]] = {
    ContactsDAO
      .getBy(issuer, lastSeenSubject, limit, groupName)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def generateToken(issuerId: Institution.Id, subjectId: Contact.Id): FutureEither[Nothing, TokenString] = {
    val token = TokenString.random()

    val tx = for {
      _ <- ConnectionTokensDAO.insert(ParticipantId(issuerId.value), token)
      _ <- ContactsDAO.setConnectionToken(Institution.Id(issuerId.value), subjectId, token)
    } yield ()

    tx.transact(xa)
      .unsafeToFuture()
      .map(_ => Right(token))
      .toFutureEither
  }
}
