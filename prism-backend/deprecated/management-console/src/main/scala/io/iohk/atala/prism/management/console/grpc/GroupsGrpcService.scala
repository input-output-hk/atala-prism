package io.iohk.atala.prism.management.console.grpc

import cats.effect.unsafe.IORuntime
import cats.syntax.either._
import cats.syntax.functor._
import io.iohk.atala.prism.auth.{AuthAndMiddlewareSupportF, AuthenticatorF}
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.services.GroupsService
import io.iohk.atala.prism.protos.{console_api, console_models}
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax.InstantToTimestampOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class GroupsGrpcService(
    groupsService: GroupsService[IOWithTraceIdContext],
    val authenticator: AuthenticatorF[ParticipantId, IOWithTraceIdContext]
)(implicit ec: ExecutionContext, runtime: IORuntime)
    extends console_api.GroupsServiceGrpc.GroupsService
    with ManagementConsoleErrorSupport
    with AuthAndMiddlewareSupportF[ManagementConsoleError, ParticipantId] {

  override protected val serviceName: String = "groups-service"
  override val IOruntime: IORuntime = runtime

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createGroup(
      request: console_api.CreateGroupRequest
  ): Future[console_api.CreateGroupResponse] =
    auth[CreateInstitutionGroup]("createGroup", request) { (institutionId, request, traceId) =>
      groupsService
        .createGroup(institutionId, request)
        .run(traceId)
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

  override def getGroups(
      request: console_api.GetGroupsRequest
  ): Future[console_api.GetGroupsResponse] =
    auth[InstitutionGroup.PaginatedQuery]("getGroups", request) { (institutionId, query, traceId) =>
      for {
        result <-
          groupsService
            .getGroups(institutionId, query)
            .run(traceId)
            .unsafeToFuture()
            .map(_.asRight)
            .toFutureEither
      } yield {
        val groupsProto =
          result.groups.map(ProtoCodecs.groupWithContactCountToProto)
        console_api.GetGroupsResponse(
          groups = groupsProto,
          totalNumberOfGroups = result.totalNumberOfRecords
        )
      }
    }

  override def updateGroup(
      request: console_api.UpdateGroupRequest
  ): Future[console_api.UpdateGroupResponse] =
    auth[UpdateInstitutionGroup]("updateGroup", request) { (institutionId, updateInstitutionGroup, traceId) =>
      groupsService
        .updateGroup(
          institutionId,
          updateInstitutionGroup
        )
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .as(console_api.UpdateGroupResponse())
    }

  override def copyGroup(
      request: console_api.CopyGroupRequest
  ): Future[console_api.CopyGroupResponse] =
    auth[CopyInstitutionGroup]("copyGroup", request) { (institutionId, copyInstitutionGroup, traceId) =>
      groupsService
        .copyGroup(institutionId, copyInstitutionGroup)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .map { createdGroup =>
          console_api.CopyGroupResponse(groupId = createdGroup.id.toString)
        }
    }

  override def deleteGroup(
      request: console_api.DeleteGroupRequest
  ): Future[console_api.DeleteGroupResponse] = {
    auth[DeleteInstitutionGroup]("deleteGroup", request) { (institutionId, deleteInstitutionGroup, traceId) =>
      groupsService
        .deleteGroup(institutionId, deleteInstitutionGroup)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .as(console_api.DeleteGroupResponse())
    }
  }
}
