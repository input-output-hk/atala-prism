package io.iohk.atala.prism.console.services

import cats.syntax.functor._
import io.iohk.atala.prism.auth.AuthSupport
import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.connector.errors.{ConnectorError, ConnectorErrorSupport, InternalServerError}
import io.iohk.atala.prism.console.grpc._
import io.iohk.atala.prism.console.models.actions.{CreateGroupRequest, GetGroupsRequest, UpdateGroupRequest}
import io.iohk.atala.prism.console.models.Institution
import io.iohk.atala.prism.console.repositories.GroupsRepository
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.console_api.{
  CopyGroupRequest,
  CopyGroupResponse,
  DeleteGroupRequest,
  DeleteGroupResponse
}
import io.iohk.atala.prism.protos.{console_api, console_models}
import io.iohk.atala.prism.utils.syntax._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class GroupsServiceImpl(issuerGroupsRepository: GroupsRepository, val authenticator: ConnectorAuthenticator)(implicit
    ec: ExecutionContext
) extends console_api.GroupsServiceGrpc.GroupsService
    with ConnectorErrorSupport
    with AuthSupport[ConnectorError, ParticipantId] {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createGroup(request: console_api.CreateGroupRequest): Future[console_api.CreateGroupResponse] =
    auth[CreateGroupRequest]("createGroup", request) { (participantId, createGroupRequest) =>
      val issuerId: Institution.Id = Institution.Id(participantId.uuid)
      issuerGroupsRepository
        .create(issuerId, createGroupRequest.name)
        .map { createdGroup =>
          console_api
            .CreateGroupResponse()
            .withGroup(
              console_models
                .Group()
                .withId(createdGroup.id.toString)
                .withCreatedAtDeprecated(createdGroup.createdAt.getEpochSecond)
                .withCreatedAt(createdGroup.createdAt.toProtoTimestamp)
                .withName(createdGroup.name.value)
                .withNumberOfContacts(0) // creating a group adds no contacts
            )
        }
    }

  override def getGroups(request: console_api.GetGroupsRequest): Future[console_api.GetGroupsResponse] =
    auth[GetGroupsRequest]("getGroups", request) { (participantId, getGroupRequest) =>
      val issuerId: Institution.Id = Institution.Id(participantId.uuid)
      issuerGroupsRepository.getBy(issuerId, getGroupRequest.maybeContactId).map { groups =>
        val proto = groups.map { g =>
          console_models
            .Group()
            .withId(g.value.id.toString)
            .withCreatedAtDeprecated(g.value.createdAt.getEpochSecond)
            .withCreatedAt(g.value.createdAt.toProtoTimestamp)
            .withName(g.value.name.value)
            .withNumberOfContacts(g.numberOfContacts)
        }
        console_api.GetGroupsResponse(proto)
      }
    }

  override def updateGroup(request: console_api.UpdateGroupRequest): Future[console_api.UpdateGroupResponse] =
    auth[UpdateGroupRequest]("updateGroup", request) { (participantId, updateGroupRequest) =>
      val institutionId = Institution.Id(participantId.uuid)
      issuerGroupsRepository
        .updateGroup(
          institutionId,
          updateGroupRequest.groupId,
          updateGroupRequest.contactsToAdd,
          updateGroupRequest.contactsToRemove
        )
        .mapLeft(e => InternalServerError(new RuntimeException(s"FAILED: $e")))
        .as(console_api.UpdateGroupResponse())
    }

  override def copyGroup(request: CopyGroupRequest): Future[CopyGroupResponse] = ???

  override def deleteGroup(request: DeleteGroupRequest): Future[DeleteGroupResponse] = ???
}
