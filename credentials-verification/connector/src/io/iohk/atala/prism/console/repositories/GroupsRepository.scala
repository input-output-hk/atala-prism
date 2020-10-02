package io.iohk.atala.prism.console.repositories

import cats.effect.IO
import cats.syntax.either._
import doobie.ConnectionIO
import doobie.free.connection
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.console.errors._
import io.iohk.atala.prism.console.models.{Contact, Institution, IssuerGroup}
import io.iohk.atala.prism.console.repositories.daos.{ContactsDAO, IssuerGroupsDAO}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class GroupsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(issuer: Institution.Id, name: IssuerGroup.Name): FutureEither[Nothing, IssuerGroup] = {
    IssuerGroupsDAO
      .create(issuer, name)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getBy(issuer: Institution.Id): FutureEither[Nothing, List[IssuerGroup.Name]] = {
    IssuerGroupsDAO
      .getBy(issuer)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def listContacts(
      issuer: Institution.Id,
      groupName: IssuerGroup.Name
  ): FutureEither[Nothing, List[Contact]] = {
    val connectionIo = for {
      groupOpt <- IssuerGroupsDAO.find(issuer, groupName)
      group = groupOpt.getOrElse(throw new RuntimeException(s"Group $groupName does not exist"))
      contacts <- IssuerGroupsDAO.listContacts(group.id)
    } yield contacts

    connectionIo.transact(xa).unsafeToFuture().map(Right(_)).toFutureEither
  }

  def updateGroup(
      issuer: Institution.Id,
      groupId: IssuerGroup.Id,
      contactIdsToAdd: List[Contact.Id],
      contactIdsToRemove: List[Contact.Id]
  ): FutureEither[ConsoleError, Unit] = {
    def checkContacts(contactIds: List[Contact.Id]): ConnectionIO[Option[ConsoleError]] = {
      ContactsDAO.findContacts(issuer, contactIds).map { validContactsToAdd =>
        val difference = contactIds.toSet.diff(validContactsToAdd.map(_.contactId).toSet)
        if (difference.nonEmpty) {
          Some(ContactsIssuersDoNotMatch(difference.toList, issuer))
        } else {
          None
        }
      }
    }

    val connectionIo = for {
      groupOpt <- IssuerGroupsDAO.find(groupId)
      result <- groupOpt match {
        case None => connection.pure(groupDoesNotExist[Unit](groupId))
        case Some(group) =>
          if (group.issuerId != issuer) {
            connection.pure(groupIssuerDoesNotMatch[Unit](group.issuerId, issuer))
          } else {
            for {
              contactsCheckToAdd <- checkContacts(contactIdsToAdd)
              contactsCheckToRemove <- checkContacts(contactIdsToRemove)
              contactsCheck = contactsCheckToAdd.orElse(contactsCheckToRemove)
              result <- contactsCheck match {
                case Some(consoleError) => connection.pure(consoleError.asLeft[Unit])
                case None =>
                  for {
                    _ <- IssuerGroupsDAO.addContacts(groupId, contactIdsToAdd)
                    _ <- IssuerGroupsDAO.removeContacts(groupId, contactIdsToRemove)
                  } yield ().asRight[ConsoleError]
              }
            } yield result
          }
      }
    } yield result

    connectionIo.transact(xa).unsafeToFuture().toFutureEither
  }
}
