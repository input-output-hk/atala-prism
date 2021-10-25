package io.iohk.atala.prism.management.console.repositories.logs

import cats.effect.BracketThrow
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models.InstitutionGroup.PaginatedQuery
import io.iohk.atala.prism.management.console.models.{Contact, GetGroupsResult, InstitutionGroup, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.InstitutionGroupsRepository
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

private[repositories] final class InstitutionGroupsRepositoryLogs[F[
    _
]: ServiceLogging[*[
  _
], InstitutionGroupsRepository[F]]: BracketThrow]
    extends InstitutionGroupsRepository[Mid[F, *]] {

  override def create(
      institutionId: ParticipantId,
      name: InstitutionGroup.Name,
      contactIds: Set[Contact.Id]
  ): Mid[F, Either[ManagementConsoleError, InstitutionGroup]] =
    in =>
      info"creating institution group $institutionId $name" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while creating institution group $e",
            r => info"creating institution group - successfully done ${r.id}"
          )
        )
        .onError(
          errorCause"encountered an error while creating institution group" (_)
        )

  override def getBy(
      institutionId: ParticipantId,
      query: PaginatedQuery
  ): Mid[F, GetGroupsResult] =
    in =>
      info"getting institution group by query $institutionId" *> in
        .flatTap(res => info"getting institution group by query - found ${res.groups.size} entities")
        .onError(
          errorCause"encountered an error while getting institution group by query" (
            _
          )
        )

  override def listContacts(
      institutionId: ParticipantId,
      groupName: InstitutionGroup.Name
  ): Mid[F, List[Contact]] =
    in =>
      info"listing contacts $institutionId $groupName" *> in
        .flatTap(res => info"listing contacts - found ${res.size} entities")
        .onError(errorCause"encountered an error while listing contacts" (_))

  override def updateGroup(
      institutionId: ParticipantId,
      groupId: InstitutionGroup.Id,
      contactIdsToAdd: Set[Contact.Id],
      contactIdsToRemove: Set[Contact.Id],
      newNameMaybe: Option[InstitutionGroup.Name]
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"updating group $institutionId $groupId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while updating group $e",
            _ => info"updating group - successfully done"
          )
        )
        .onError(errorCause"encountered an error while updating group" (_))

  override def copyGroup(
      institutionId: ParticipantId,
      originalGroupId: InstitutionGroup.Id,
      newGroupName: InstitutionGroup.Name
  ): Mid[F, Either[ManagementConsoleError, InstitutionGroup]] =
    in =>
      info"copying group $institutionId $originalGroupId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while copying group $e",
            _ => info"copying group - successfully done"
          )
        )
        .onError(errorCause"encountered an error while copying group" (_))

  override def deleteGroup(
      institutionId: ParticipantId,
      groupId: InstitutionGroup.Id
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"deleting group $institutionId $groupId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while deleting group $e",
            _ => info"deleting group - successfully done"
          )
        )
        .onError(errorCause"encountered an error while deleting group" (_))
}
