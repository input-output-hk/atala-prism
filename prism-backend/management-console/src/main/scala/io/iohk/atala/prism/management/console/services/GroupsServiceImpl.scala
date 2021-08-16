package io.iohk.atala.prism.management.console.services

import cats.implicits.{catsSyntaxEitherId, toFunctorOps}
import io.iohk.atala.prism.auth.AuthAndMiddlewareSupport
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.grpc._
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.InstitutionGroupsRepository
import io.iohk.atala.prism.protos.{console_api, console_models}
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class GroupsServiceImpl(
    institutionGroupsRepository: InstitutionGroupsRepository[IOWithTraceIdContext],
    val authenticator: ManagementConsoleAuthenticator
)(implicit
    ec: ExecutionContext
) extends console_api.GroupsServiceGrpc.GroupsService
    with ManagementConsoleErrorSupport
    with AuthAndMiddlewareSupport[ManagementConsoleError, ParticipantId] {

  override protected val serviceName: String = "groups-service"

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createGroup(request: console_api.CreateGroupRequest): Future[console_api.CreateGroupResponse] =
    auth[CreateInstitutionGroup]("createGroup", request) { (institutionId, request) =>
      institutionGroupsRepository
        .create(
          institutionId,
          request.name,
          request.contactIds
        )
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .toFutureEither
        .map { group =>
          console_api
            .CreateGroupResponse()
            .withGroup(
              console_models
                .Group()
                .withId(group.id.toString)
                .withCreatedAt(group.createdAt.toProtoTimestamp)
                .withName(group.name.value)
                .withNumberOfContacts(request.contactIds.size)
            )
        }
    }

  override def getGroups(request: console_api.GetGroupsRequest): Future[console_api.GetGroupsResponse] =
    auth[InstitutionGroup.PaginatedQuery]("getGroups", request) { (institutionId, query) =>
      for {
        result <-
          institutionGroupsRepository
            .getBy(institutionId, query)
            .run(TraceId.generateYOLO)
            .unsafeToFuture()
            .map(_.asRight)
            .toFutureEither
        (groups, totalNumberOfRecords) = result
      } yield {
        val groupsProto = groups.map(ProtoCodecs.groupWithContactCountToProto)
        console_api.GetGroupsResponse(groups = groupsProto, totalNumberOfGroups = totalNumberOfRecords)
      }
    }

  override def updateGroup(request: console_api.UpdateGroupRequest): Future[console_api.UpdateGroupResponse] =
    auth[UpdateInstitutionGroup]("updateGroup", request) { (institutionId, updateInstitutionGroup) =>
      institutionGroupsRepository
        .updateGroup(
          institutionId,
          updateInstitutionGroup.groupId,
          updateInstitutionGroup.contactIdsToAdd,
          updateInstitutionGroup.contactIdsToRemove,
          updateInstitutionGroup.name
        )
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .toFutureEither
        .as(console_api.UpdateGroupResponse())
    }

  override def copyGroup(request: console_api.CopyGroupRequest): Future[console_api.CopyGroupResponse] =
    auth[CopyInstitutionGroup]("copyGroup", request) { (institutionId, copyInstitutionGroup) =>
      institutionGroupsRepository
        .copyGroup(institutionId, copyInstitutionGroup.groupId, copyInstitutionGroup.newName)
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .toFutureEither
        .map { createdGroup =>
          console_api.CopyGroupResponse(groupId = createdGroup.id.toString)
        }
    }

  override def deleteGroup(request: console_api.DeleteGroupRequest): Future[console_api.DeleteGroupResponse] = {
    auth[DeleteInstitutionGroup]("deleteGroup", request) { (institutionId, deleteInstitutionGroup) =>
      institutionGroupsRepository
        .deleteGroup(institutionId, deleteInstitutionGroup.groupId)
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .toFutureEither
        .as(console_api.DeleteGroupResponse())
    }
  }
}
