package io.iohk.atala.prism.management.console.services

import cats.{Comonad, Functor, Monad}
import cats.effect.Resource
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.comonad._
import cats.syntax.flatMap._
import cats.syntax.functor._
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models.InstitutionGroup.PaginatedQuery
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.InstitutionGroupsRepository
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.logging._
import cats.MonadThrow
import cats.effect.MonadCancelThrow

@derive(applyK)
trait GroupsService[F[_]] {
  def createGroup(
      institutionId: ParticipantId,
      createInstitutionGroup: CreateInstitutionGroup
  ): F[Either[ManagementConsoleError, InstitutionGroup]]

  def getGroups(
      institutionId: ParticipantId,
      query: InstitutionGroup.PaginatedQuery
  ): F[GetGroupsResult]

  def updateGroup(
      institutionId: ParticipantId,
      updateInstitutionGroup: UpdateInstitutionGroup
  ): F[Either[ManagementConsoleError, Unit]]

  def copyGroup(
      institutionId: ParticipantId,
      copyInstitutionGroup: CopyInstitutionGroup
  ): F[Either[ManagementConsoleError, InstitutionGroup]]

  def deleteGroup(
      institutionId: ParticipantId,
      deleteInstitutionGroup: DeleteInstitutionGroup
  ): F[Either[ManagementConsoleError, Unit]]

}

object GroupsService {
  def apply[F[_]: TimeMeasureMetric: MonadThrow, R[_]: Functor](
      institutionGroupsRepository: InstitutionGroupsRepository[F],
      logs: Logs[R, F]
  ): R[GroupsService[F]] =
    for {
      serviceLogs <- logs.service[GroupsService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, GroupsService[F]] =
        serviceLogs
      val logs: GroupsService[Mid[F, *]] = new GroupsServiceLogs[F]
      val mid = logs
      mid attach new GroupsServiceImpl[F](institutionGroupsRepository)
    }

  def unsafe[F[_]: TimeMeasureMetric: MonadCancelThrow, R[_]: Comonad](
      institutionGroupsRepository: InstitutionGroupsRepository[F],
      logs: Logs[R, F]
  ): GroupsService[F] = GroupsService(institutionGroupsRepository, logs).extract

  def makeResource[F[_]: TimeMeasureMetric: MonadCancelThrow, R[_]: Monad](
      institutionGroupsRepository: InstitutionGroupsRepository[F],
      logs: Logs[R, F]
  ): Resource[R, GroupsService[F]] =
    Resource.eval(
      GroupsService(institutionGroupsRepository, logs)
    )
}

private final class GroupsServiceImpl[F[_]](
    institutionGroupsRepository: InstitutionGroupsRepository[F]
) extends GroupsService[F] {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createGroup(
      institutionId: ParticipantId,
      createInstitutionGroup: CreateInstitutionGroup
  ): F[Either[ManagementConsoleError, InstitutionGroup]] =
    institutionGroupsRepository.create(
      institutionId,
      createInstitutionGroup.name,
      createInstitutionGroup.contactIds
    )

  override def getGroups(
      institutionId: ParticipantId,
      query: InstitutionGroup.PaginatedQuery
  ): F[GetGroupsResult] =
    institutionGroupsRepository
      .getBy(institutionId, query)

  override def updateGroup(
      institutionId: ParticipantId,
      updateInstitutionGroup: UpdateInstitutionGroup
  ): F[Either[ManagementConsoleError, Unit]] =
    institutionGroupsRepository
      .updateGroup(
        institutionId,
        updateInstitutionGroup.groupId,
        updateInstitutionGroup.contactIdsToAdd,
        updateInstitutionGroup.contactIdsToRemove,
        updateInstitutionGroup.name
      )

  override def copyGroup(
      institutionId: ParticipantId,
      copyInstitutionGroup: CopyInstitutionGroup
  ): F[Either[ManagementConsoleError, InstitutionGroup]] =
    institutionGroupsRepository
      .copyGroup(
        institutionId,
        copyInstitutionGroup.groupId,
        copyInstitutionGroup.newName
      )

  override def deleteGroup(
      institutionId: ParticipantId,
      deleteInstitutionGroup: DeleteInstitutionGroup
  ): F[Either[ManagementConsoleError, Unit]] =
    institutionGroupsRepository
      .deleteGroup(institutionId, deleteInstitutionGroup.groupId)
}

private final class GroupsServiceLogs[
    F[_]: ServiceLogging[*[_], GroupsService[F]]: MonadThrow
] extends GroupsService[Mid[F, *]] {
  override def createGroup(
      institutionId: ParticipantId,
      createInstitutionGroup: CreateInstitutionGroup
  ): Mid[F, Either[ManagementConsoleError, InstitutionGroup]] =
    in =>
      info"creating institution group $institutionId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while creating institution group $e",
            _ => info"creating institution group - successfully done"
          )
        )
        .onError(
          errorCause"encountered an error while creating institution group" (_)
        )

  override def getGroups(
      institutionId: ParticipantId,
      query: PaginatedQuery
  ): Mid[F, GetGroupsResult] =
    in =>
      info"getting institution group by query $institutionId" *> in
        .flatTap(_ => info"getting institution group by query - successfully done")
        .onError(
          errorCause"encountered an error while getting institution group by query" (
            _
          )
        )

  override def updateGroup(
      institutionId: ParticipantId,
      updateInstitutionGroup: UpdateInstitutionGroup
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"updating group $institutionId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while updating group $e",
            _ => info"updating group - successfully done"
          )
        )
        .onError(errorCause"encountered an error while updating group" (_))

  override def copyGroup(
      institutionId: ParticipantId,
      copyInstitutionGroup: CopyInstitutionGroup
  ): Mid[F, Either[ManagementConsoleError, InstitutionGroup]] =
    in =>
      info"copying group $institutionId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while copying group $e",
            _ => info"copying group - successfully done"
          )
        )
        .onError(errorCause"encountered an error while copying group" (_))

  override def deleteGroup(
      institutionId: ParticipantId,
      deleteInstitutionGroup: DeleteInstitutionGroup
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"deleting group $institutionId" *> in
        .flatTap(
          _.fold(
            e => error"encountered an error while deleting group $e",
            _ => info"deleting group - successfully done"
          )
        )
        .onError(errorCause"encountered an error while deleting group" (_))
}
