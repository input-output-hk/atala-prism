package io.iohk.atala.prism.management.console.repositories

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.apply._
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import doobie.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.errors._
import io.iohk.atala.prism.management.console.models.{Contact, InstitutionGroup, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.daos.InstitutionGroupsDAO
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

class InstitutionGroupsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  val logger: Logger = LoggerFactory.getLogger(getClass)

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

    transaction.value
      .logSQLErrors(s"creating, institution id - $institutionId", logger)
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither
  }

  def getBy(
      institutionId: ParticipantId,
      query: InstitutionGroup.PaginatedQuery
  ): FutureEither[Nothing, (List[InstitutionGroup.WithContactCount], Int)] = {
    (for {
      groups <- InstitutionGroupsDAO.getBy(institutionId, query)
      totalNumberOfRecords <- InstitutionGroupsDAO.getTotalNumberOfRecords(institutionId, query)
    } yield groups -> totalNumberOfRecords)
      .logSQLErrors(s"getting, institution id - $institutionId", logger)
      .transact(xa)
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

    connectionIo
      .logSQLErrors(s"list contacts, institution id - $institutionId", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
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

    transaction.value
      .logSQLErrors(s"updating, group id - $groupId", logger)
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither
  }

  def copyGroup(
      institutionId: ParticipantId,
      originalGroupId: InstitutionGroup.Id,
      newGroupName: InstitutionGroup.Name
  ): FutureEither[ManagementConsoleError, InstitutionGroup] = {
    import institutionHelper._

    val connectionIo = for {
      _ <- EitherT.fromOptionF(checkGroups(institutionId, Set(originalGroupId)), ()).swap
      _ <- EitherT.fromOptionF(checkGroupNameIsFree(institutionId, newGroupName), ()).swap
      createdGroup <- EitherT.right[ManagementConsoleError](InstitutionGroupsDAO.create(institutionId, newGroupName))
      _ <- EitherT.right[ManagementConsoleError](InstitutionGroupsDAO.copyContacts(originalGroupId, createdGroup.id))
    } yield createdGroup

    connectionIo.value
      .logSQLErrors(s"copying group, institution id - $institutionId", logger)
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither
  }

  def deleteGroup(
      institutionId: ParticipantId,
      groupId: InstitutionGroup.Id
  ): FutureEither[ManagementConsoleError, Unit] = {
    import institutionHelper._

    val connectionIo = for {
      _ <- EitherT.fromOptionF(checkGroups(institutionId, Set(groupId)), ()).swap
      _ <- EitherT.right[ManagementConsoleError](InstitutionGroupsDAO.removeAllGroupContacts(groupId))
      _ <- EitherT(
        InstitutionGroupsDAO
          .deleteGroup(institutionId, groupId)
          .ifM(
            ().asRight[ManagementConsoleError].pure[ConnectionIO],
            (InternalServerError(
              new Throwable(s"Can't delete group with id $groupId for institution $institutionId")
            ): ManagementConsoleError)
              .asLeft[Unit]
              .pure[ConnectionIO]
          )
      )
    } yield ()

    connectionIo.value
      .logSQLErrors(s"deleting, group id - $groupId", logger)
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither
  }
}
