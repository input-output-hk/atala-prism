package io.iohk.atala.prism.management.console.repositories.metrics

import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models.InstitutionGroup.PaginatedQuery
import io.iohk.atala.prism.management.console.models.{Contact, GetGroupsResult, InstitutionGroup, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.InstitutionGroupsRepository
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import tofu.higherKind.Mid
import cats.effect.MonadCancelThrow

final class InstitutionGroupsRepositoryMetrics[F[
    _
]: TimeMeasureMetric: MonadCancelThrow]
    extends InstitutionGroupsRepository[Mid[F, *]] {
  private val repoName = "InstitutionGroupsRepository"
  private lazy val createTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "create")
  private lazy val getByTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getBy")
  private lazy val listContactsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "listContacts")
  private lazy val updateGroupTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "updateGroup")
  private lazy val copyGroupTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "copyGroup")
  private lazy val deleteGroupTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "deleteGroup")

  override def create(
      institutionId: ParticipantId,
      name: InstitutionGroup.Name,
      contactIds: Set[Contact.Id]
  ): Mid[F, Either[ManagementConsoleError, InstitutionGroup]] =
    _.measureOperationTime(createTimer)

  override def getBy(
      institutionId: ParticipantId,
      query: PaginatedQuery
  ): Mid[F, GetGroupsResult] = _.measureOperationTime(getByTimer)

  override def listContacts(
      institutionId: ParticipantId,
      groupName: InstitutionGroup.Name
  ): Mid[F, List[Contact]] =
    _.measureOperationTime(listContactsTimer)

  override def updateGroup(
      institutionId: ParticipantId,
      groupId: InstitutionGroup.Id,
      contactIdsToAdd: Set[Contact.Id],
      contactIdsToRemove: Set[Contact.Id],
      newNameMaybe: Option[InstitutionGroup.Name]
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    _.measureOperationTime(updateGroupTimer)

  override def copyGroup(
      institutionId: ParticipantId,
      originalGroupId: InstitutionGroup.Id,
      newGroupName: InstitutionGroup.Name
  ): Mid[F, Either[ManagementConsoleError, InstitutionGroup]] =
    _.measureOperationTime(copyGroupTimer)

  override def deleteGroup(
      institutionId: ParticipantId,
      groupId: InstitutionGroup.Id
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    _.measureOperationTime(deleteGroupTimer)
}
