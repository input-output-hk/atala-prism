package io.iohk.atala.prism.management.console.services

import java.util.UUID

import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.models.{Contact, InstitutionGroup, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.InstitutionGroupsRepository
import io.iohk.atala.prism.protos.{console_api, console_models}
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

class GroupsServiceImpl(
    institutionGroupsRepository: InstitutionGroupsRepository,
    authenticator: ManagementConsoleAuthenticator
)(implicit
    ec: ExecutionContext
) extends console_api.GroupsServiceGrpc.GroupsService {

  override def createGroup(request: console_api.CreateGroupRequest): Future[console_api.CreateGroupResponse] = {

    def f(institutionId: ParticipantId) = {
      institutionGroupsRepository
        .create(institutionId, InstitutionGroup.Name(request.name))
        .value
        .map {
          case Right(g) =>
            console_api
              .CreateGroupResponse()
              .withGroup(
                console_models
                  .Group()
                  .withId(g.id.value.toString)
                  .withCreatedAt(g.createdAt.getEpochSecond)
                  .withName(g.name.value)
                  .withNumberOfContacts(0) // creating a group adds no contacts
              )
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }
    authenticator.authenticated("createGroup", request) { institutionId =>
      f(institutionId)
    }

  }

  override def getGroups(request: console_api.GetGroupsRequest): Future[console_api.GetGroupsResponse] = {

    def f(institutionId: ParticipantId) = {
      lazy val contactIdT = if (request.contactId.nonEmpty) {
        Try(UUID.fromString(request.contactId))
          .orElse(Failure(new RuntimeException("The provided contactId is invalid")))
          .map(Contact.Id.apply)
          .map(Option.apply)
          .map(Right(_))
      } else Try(Right(None))

      for {
        contactIdMaybe <- Future.fromTry(contactIdT).toFutureEither
        groups <- institutionGroupsRepository.getBy(institutionId, contactIdMaybe)
      } yield {
        val proto = groups.map { g =>
          console_models
            .Group()
            .withId(g.value.id.value.toString)
            .withCreatedAt(g.value.createdAt.getEpochSecond)
            .withName(g.value.name.value)
            .withNumberOfContacts(g.numberOfContacts)
        }
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
    def f(institutionId: ParticipantId) = {
      institutionGroupsRepository
        .updateGroup(
          institutionId,
          InstitutionGroup.Id(UUID.fromString(request.groupId)),
          request.contactIdsToAdd.map(id => Contact.Id(UUID.fromString(id))).to(List),
          request.contactIdsToRemove.map(id => Contact.Id(UUID.fromString(id))).to(List)
        )
        .value
        .map {
          case Right(_) => console_api.UpdateGroupResponse()
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }

    authenticator.authenticated("updateGroups", request) { institutionId =>
      f(institutionId)
    }
  }
}
