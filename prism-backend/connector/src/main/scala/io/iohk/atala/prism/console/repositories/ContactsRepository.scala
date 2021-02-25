package io.iohk.atala.prism.console.repositories

import cats.effect.IO
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.traverse._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.errors.{ConnectorError, NotFoundByFieldError}
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.connector.repositories.daos.ConnectionTokensDAO
import io.iohk.atala.prism.console.models.{Contact, CreateContact, Institution, IssuerGroup}
import io.iohk.atala.prism.console.repositories.daos.{ContactsDAO, IssuerGroupsDAO}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class ContactsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(
      contactData: CreateContact,
      maybeGroupName: Option[IssuerGroup.Name]
  ): FutureEither[ConnectorError, Contact] = {
    val query: ConnectionIO[Either[ConnectorError, Contact]] = maybeGroupName match {
      case None => // if we do not request the subject to be added to a group
        ContactsDAO.createContact(contactData).asRight[ConnectorError].sequence
      case Some(groupName) => // if we are requesting to add a subject to a group
        for {
          groupMaybe <- IssuerGroupsDAO.find(contactData.createdBy, groupName)
          result <-
            groupMaybe
              .fold( //If a group with a passed name is not found
                NotFoundByFieldError("group", "name", groupName.value).asLeft[ConnectionIO[Contact]]
              ) { group =>
                ContactsDAO
                  .createContact(contactData)
                  .flatMap(contact => IssuerGroupsDAO.addContact(group.id, contact.contactId).as(contact))
                  .asRight
              }
              .sequence
        } yield result
    }

    query
      .transact(xa)
      .unsafeToFuture()
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
      createdBy: Institution.Id,
      lastSeen: Option[Contact.Id],
      groupName: Option[IssuerGroup.Name],
      limit: Int
  ): FutureEither[ConnectorError, Seq[Contact]] = {
    ContactsDAO
      .getBy(createdBy, lastSeen, limit, groupName)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def generateToken(issuerId: Institution.Id, contactId: Contact.Id): FutureEither[Nothing, TokenString] = {
    val token = TokenString.random()

    val tx = for {
      _ <- ConnectionTokensDAO.insert(ParticipantId(issuerId.uuid), token)
      _ <- ContactsDAO.setConnectionToken(issuerId, contactId, token)
    } yield ()

    tx.transact(xa)
      .unsafeToFuture()
      .map(_ => Right(token))
      .toFutureEither
  }
}
