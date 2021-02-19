package io.iohk.atala.prism.management.console.services

import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{
  CreateGroupInvalidRequest,
  DeleteGroupInvalidRequest,
  ManagementConsoleErrorSupport,
  UpdateGroupInvalidRequest
}
import io.iohk.atala.prism.management.console.grpc.ProtoCodecs
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CreateInstitutionGroup,
  DeleteInstitutionGroup,
  ParticipantId,
  UpdateInstitutionGroup
}
import io.iohk.atala.prism.management.console.repositories.InstitutionGroupsRepository
import io.iohk.atala.prism.protos.console_api.{DeleteGroupRequest, DeleteGroupResponse}
import io.iohk.atala.prism.protos.{console_api, console_models}
import io.iohk.atala.prism.utils.FutureEither
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class GroupsServiceImpl(
    institutionGroupsRepository: InstitutionGroupsRepository,
    authenticator: ManagementConsoleAuthenticator
)(implicit
    ec: ExecutionContext
) extends console_api.GroupsServiceGrpc.GroupsService
    with ManagementConsoleErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createGroup(request: console_api.CreateGroupRequest): Future[console_api.CreateGroupResponse] = {

    def f(institutionId: ParticipantId, request: CreateInstitutionGroup) = {
      institutionGroupsRepository
        .create(
          institutionId,
          request.name,
          request.contactIds
        )
        .value
        .map {
          case Right(g) =>
            console_api
              .CreateGroupResponse()
              .withGroup(
                console_models
                  .Group()
                  .withId(g.id.toString)
                  .withCreatedAt(g.createdAt.getEpochSecond)
                  .withName(g.name.value)
                  .withNumberOfContacts(request.contactIds.size)
              )
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }

    authenticator.authenticated("createGroup", request) { institutionId =>
      ProtoCodecs.toCreateGroup(request) match {
        case Failure(exception) =>
          val response = CreateGroupInvalidRequest(exception.getMessage)
          respondWith(request, response)
        case Success(createInstitutionGroup) =>
          f(institutionId, createInstitutionGroup)
      }
    }
  }

  override def getGroups(request: console_api.GetGroupsRequest): Future[console_api.GetGroupsResponse] = {

    def f(institutionId: ParticipantId) = {
      lazy val contactIdT = if (request.contactId.nonEmpty) {
        Contact.Id.from(request.contactId).map(Option.apply)
      } else Try(None)

      for {
        contactIdMaybe <- FutureEither(contactIdT)
        groups <- institutionGroupsRepository.getBy(institutionId, contactIdMaybe)
      } yield {
        val proto = groups.map(ProtoCodecs.groupWithContactCountToProto)
        console_api.GetGroupsResponse(proto)
      }
    }

    authenticator.authenticated("getGroups", request) { participantId =>
      f(participantId).value
        .map {
          case Right(value) => value
          case Left(_) => throw new RuntimeException("Unknown error while retrieving groups")
        }
    }
  }

  override def updateGroup(request: console_api.UpdateGroupRequest): Future[console_api.UpdateGroupResponse] = {
    def f(institutionId: ParticipantId, updateInstitutionGroup: UpdateInstitutionGroup) = {
      institutionGroupsRepository
        .updateGroup(
          institutionId,
          updateInstitutionGroup.groupId,
          updateInstitutionGroup.contactIdsToAdd,
          updateInstitutionGroup.contactIdsToRemove,
          updateInstitutionGroup.name
        )
        .value
        .map {
          case Right(_) => console_api.UpdateGroupResponse()
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }

    authenticator.authenticated("updateGroups", request) { institutionId =>
      ProtoCodecs.toUpdateGroup(request) match {
        case Failure(exception) =>
          val response = UpdateGroupInvalidRequest(exception.getMessage)
          respondWith(request, response)
        case Success(updateInstitutionGroup) =>
          f(institutionId, updateInstitutionGroup)
      }
    }
  }

  override def deleteGroup(request: DeleteGroupRequest): Future[DeleteGroupResponse] = {
    def f(institutionId: ParticipantId, deleteInstitutionGroup: DeleteInstitutionGroup) = {
      institutionGroupsRepository
        .deleteGroup(institutionId, deleteInstitutionGroup.groupId)
        .value
        .map {
          case Right(_) => console_api.DeleteGroupResponse()
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }

    authenticator.authenticated("deleteGroup", request) { institutionId =>
      ProtoCodecs.toDeleteGroup(request) match {
        case Failure(exception) =>
          val response = DeleteGroupInvalidRequest(exception.getMessage)
          respondWith(request, response)
        case Success(deleteInstitutionGroup) =>
          f(institutionId, deleteInstitutionGroup)
      }
    }
  }
}
