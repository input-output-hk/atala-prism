package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import cats.syntax.either._
import doobie.ConnectionIO
import doobie.free.connection
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.errors._
import io.iohk.atala.prism.management.console.models.{Contact, ParticipantId, InstitutionGroup}
import io.iohk.atala.prism.management.console.repositories.daos.{ContactsDAO, InstitutionGroupsDAO}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class InstitutionGroupsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(institutionId: ParticipantId, name: InstitutionGroup.Name): FutureEither[Nothing, InstitutionGroup] = {
    InstitutionGroupsDAO
      .create(institutionId, name)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getBy(
      institutionId: ParticipantId,
      filterByContact: Option[Contact.Id]
  ): FutureEither[Nothing, List[InstitutionGroup.WithContactCount]] = {
    val tx = filterByContact match {
      case Some(contactId) => InstitutionGroupsDAO.getBy(institutionId, contactId)
      case None => InstitutionGroupsDAO.getBy(institutionId)
    }

    tx.transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def listContacts(
      institutionId: ParticipantId,
      groupName: InstitutionGroup.Name
  ): FutureEither[Nothing, List[Contact]] = {
    val connectionIo = for {
      groupOpt <- InstitutionGroupsDAO.find(institutionId, groupName)
      group = groupOpt.getOrElse(throw new RuntimeException(s"Group $groupName does not exist"))
      contacts <- InstitutionGroupsDAO.listContacts(group.id)
    } yield contacts

    connectionIo.transact(xa).unsafeToFuture().map(Right(_)).toFutureEither
  }

  def updateGroup(
      institutionId: ParticipantId,
      groupId: InstitutionGroup.Id,
      contactIdsToAdd: List[Contact.Id],
      contactIdsToRemove: List[Contact.Id]
  ): FutureEither[ManagementConsoleError, Unit] = {
    def checkContacts(contactIds: List[Contact.Id]): ConnectionIO[Option[ManagementConsoleError]] = {
      ContactsDAO.findContacts(institutionId, contactIds).map { validContactsToAdd =>
        val difference = contactIds.toSet.diff(validContactsToAdd.map(_.contactId).toSet)
        if (difference.nonEmpty) {
          Some(ContactsInstitutionsDoNotMatch(difference.toList, institutionId))
        } else {
          None
        }
      }
    }

    val connectionIo = for {
      groupOpt <- InstitutionGroupsDAO.find(groupId)
      result <- groupOpt match {
        case None => connection.pure(groupDoesNotExist[Unit](groupId))
        case Some(group) =>
          if (group.institutionId != institutionId) {
            connection.pure(groupInstitutionDoesNotMatch[Unit](group.institutionId, institutionId))
          } else {
            for {
              contactsCheckToAdd <- checkContacts(contactIdsToAdd)
              contactsCheckToRemove <- checkContacts(contactIdsToRemove)
              contactsCheck = contactsCheckToAdd.orElse(contactsCheckToRemove)
              result <- contactsCheck match {
                case Some(consoleError) => connection.pure(consoleError.asLeft[Unit])
                case None =>
                  for {
                    _ <- InstitutionGroupsDAO.addContacts(groupId, contactIdsToAdd)
                    _ <- InstitutionGroupsDAO.removeContacts(groupId, contactIdsToRemove)
                  } yield ().asRight[ManagementConsoleError]
              }
            } yield result
          }
      }
    } yield result

    connectionIo.transact(xa).unsafeToFuture().toFutureEither
  }
}
