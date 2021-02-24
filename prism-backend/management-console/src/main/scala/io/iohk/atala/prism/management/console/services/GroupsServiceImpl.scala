package io.iohk.atala.prism.management.console.services

import io.iohk.atala.prism.auth.AuthSupport
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.grpc._
import io.iohk.atala.prism.management.console.models.{
  CopyInstitutionGroup,
  CreateInstitutionGroup,
  DeleteInstitutionGroup,
  GetInstitutionGroups,
  ParticipantId,
  UpdateInstitutionGroup
}
import io.iohk.atala.prism.management.console.repositories.InstitutionGroupsRepository
import io.iohk.atala.prism.protos.{console_api, console_models}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class GroupsServiceImpl(
    institutionGroupsRepository: InstitutionGroupsRepository,
    val authenticator: ManagementConsoleAuthenticator
)(implicit
    ec: ExecutionContext
) extends console_api.GroupsServiceGrpc.GroupsService
    with ManagementConsoleErrorSupport
    with AuthSupport[ManagementConsoleError, ParticipantId] {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createGroup(request: console_api.CreateGroupRequest): Future[console_api.CreateGroupResponse] =
    auth[CreateInstitutionGroup]("createGroup", request) { (institutionId, request) =>
      institutionGroupsRepository
        .create(
          institutionId,
          request.name,
          request.contactIds
        )
        .map { group =>
          console_api
            .CreateGroupResponse()
            .withGroup(
              console_models
                .Group()
                .withId(group.id.toString)
                .withCreatedAt(group.createdAt.getEpochSecond)
                .withName(group.name.value)
                .withNumberOfContacts(request.contactIds.size)
            )
        }
    }

  override def getGroups(request: console_api.GetGroupsRequest): Future[console_api.GetGroupsResponse] =
    auth[GetInstitutionGroups]("getGroups", request) { (institutionId, request) =>
      for {
        groups <- institutionGroupsRepository.getBy(institutionId, request.contactId)
      } yield {
        val proto = groups.map(ProtoCodecs.groupWithContactCountToProto)
        console_api.GetGroupsResponse(proto)
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
        .map { _ =>
          console_api.UpdateGroupResponse()
        }
    }

  override def copyGroup(request: console_api.CopyGroupRequest): Future[console_api.CopyGroupResponse] =
    auth[CopyInstitutionGroup]("copyGroup", request) { (institutionId, copyInstitutionGroup) =>
      institutionGroupsRepository
        .copyGroup(institutionId, copyInstitutionGroup.groupId, copyInstitutionGroup.newName)
        .map { createdGroup =>
          console_api.CopyGroupResponse(groupId = createdGroup.id.toString)
        }
    }

  override def deleteGroup(request: console_api.DeleteGroupRequest): Future[console_api.DeleteGroupResponse] = {
    auth[DeleteInstitutionGroup]("deleteGroup", request) { (institutionId, deleteInstitutionGroup) =>
      institutionGroupsRepository
        .deleteGroup(institutionId, deleteInstitutionGroup.groupId)
        .map { _ =>
          console_api.DeleteGroupResponse()
        }
    }
  }
}
