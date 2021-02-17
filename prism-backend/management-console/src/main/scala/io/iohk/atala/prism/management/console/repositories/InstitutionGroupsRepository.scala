package io.iohk.atala.prism.management.console.repositories

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.apply._
import doobie.ConnectionIO
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
      contactIdsToRemove: Set[Contact.Id],
      newNameMaybe: Option[InstitutionGroup.Name]
  ): FutureEither[ManagementConsoleError, Unit] = {
    import institutionHelper._

    val transaction = for {
      group <- EitherT.fromOptionF(
        InstitutionGroupsDAO.find(groupId),
        GroupDoesNotExist(groupId): ManagementConsoleError
      )
      _ <- EitherT.fromOptionF(checkGroupInstitution(institutionId, group), ()).swap
      _ <- EitherT.fromOptionF(checkContacts(institutionId, contactIdsToAdd), ()).swap
      _ <- EitherT.fromOptionF(checkContacts(institutionId, contactIdsToRemove), ()).swap
      _ <- newNameMaybe match {
        case Some(newName) =>
          // Check that the new name is free and update the group accordingly
          EitherT.fromOptionF(checkGroupNameIsFree(institutionId, newName), ()).swap *>
            EitherT.right(InstitutionGroupsDAO.update(groupId, newName))
        case None =>
          EitherT.rightT[ConnectionIO, ManagementConsoleError](())
      }
      _ <- EitherT.right[ManagementConsoleError](
        InstitutionGroupsDAO.addContacts(Set(groupId), contactIdsToAdd)
      )
      _ <- EitherT.right[ManagementConsoleError](
        InstitutionGroupsDAO.removeContacts(groupId, contactIdsToRemove.toList)
      )
    } yield ()

    transaction.transact(xa).value.unsafeToFuture().toFutureEither
  }
}
