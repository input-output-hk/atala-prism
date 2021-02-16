package io.iohk.atala.prism.management.console.repositories

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.either._
import doobie.free.connection
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.errors._
import io.iohk.atala.prism.management.console.models.{Contact, InstitutionGroup, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.daos.InstitutionGroupsDAO
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class InstitutionGroupsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(
      institutionId: ParticipantId,
      name: InstitutionGroup.Name,
      contactIds: Set[Contact.Id]
  ): FutureEither[ManagementConsoleError, InstitutionGroup] = {
    import institutionHelper._

    val transaction = for {
      _ <- EitherT.fromOptionF(checkContacts(institutionId, contactIds), ()).swap
      institutionGroup <- EitherT.right[ManagementConsoleError](
        InstitutionGroupsDAO.create(institutionId, name)
      )
      _ <- EitherT.right[ManagementConsoleError](
        InstitutionGroupsDAO.addContacts(Set(institutionGroup.id), contactIds)
      )
    } yield institutionGroup

    transaction
      .transact(xa)
      .value
      .unsafeToFuture()
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
      contactIdsToAdd: Set[Contact.Id],
      contactIdsToRemove: Set[Contact.Id]
  ): FutureEither[ManagementConsoleError, Unit] = {
    val connectionIo = for {
      groupOpt <- InstitutionGroupsDAO.find(groupId)
      result <- groupOpt match {
        case None => connection.pure(groupDoesNotExist[Unit](groupId))
        case Some(group) =>
          if (group.institutionId != institutionId) {
            connection.pure(groupInstitutionDoesNotMatch[Unit](group.institutionId, institutionId))
          } else {
            for {
              contactsCheckToAdd <- institutionHelper.checkContacts(institutionId, contactIdsToAdd)
              contactsCheckToRemove <- institutionHelper.checkContacts(institutionId, contactIdsToRemove)
              contactsCheck = contactsCheckToAdd.orElse(contactsCheckToRemove)
              result <- contactsCheck match {
                case Some(consoleError) => connection.pure(consoleError.asLeft[Unit])
                case None =>
                  for {
                    _ <- InstitutionGroupsDAO.addContacts(Set(groupId), contactIdsToAdd)
                    _ <- InstitutionGroupsDAO.removeContacts(groupId, contactIdsToRemove.toList)
                  } yield ().asRight[ManagementConsoleError]
              }
            } yield result
          }
      }
    } yield result

    connectionIo.transact(xa).unsafeToFuture().toFutureEither
  }
}
