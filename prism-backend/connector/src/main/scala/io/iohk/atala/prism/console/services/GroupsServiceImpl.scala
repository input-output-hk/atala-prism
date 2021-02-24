package io.iohk.atala.prism.console.services

import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.console.models.{Contact, Institution, IssuerGroup}
import io.iohk.atala.prism.console.repositories.GroupsRepository
import io.iohk.atala.prism.protos.console_api.{
  CopyGroupRequest,
  CopyGroupResponse,
  DeleteGroupRequest,
  DeleteGroupResponse
}
import io.iohk.atala.prism.protos.{console_api, console_models}
import io.iohk.atala.prism.utils.FutureEither

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class GroupsServiceImpl(issuerGroupsRepository: GroupsRepository, authenticator: ConnectorAuthenticator)(implicit
    ec: ExecutionContext
) extends console_api.GroupsServiceGrpc.GroupsService {

  override def createGroup(request: console_api.CreateGroupRequest): Future[console_api.CreateGroupResponse] = {

    def f(issuerId: Institution.Id) = {
      issuerGroupsRepository
        .create(issuerId, IssuerGroup.Name(request.name))
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
                  .withNumberOfContacts(0) // creating a group adds no contacts
              )
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }
    authenticator.authenticated("createGroup", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }

  }

  override def getGroups(request: console_api.GetGroupsRequest): Future[console_api.GetGroupsResponse] = {

    def f(issuerId: Institution.Id) = {
      lazy val contactIdT = if (request.contactId.nonEmpty) {
        Contact.Id.from(request.contactId).map(Option.apply)
      } else Try(None)

      for {
        contactIdMaybe <- FutureEither(contactIdT)
        groups <- issuerGroupsRepository.getBy(issuerId, contactIdMaybe)
      } yield {
        val proto = groups.map { g =>
          console_models
            .Group()
            .withId(g.value.id.toString)
            .withCreatedAt(g.value.createdAt.getEpochSecond)
            .withName(g.value.name.value)
            .withNumberOfContacts(g.numberOfContacts)
        }
        console_api.GetGroupsResponse(proto)
      }
    }

    authenticator.authenticated("getGroups", request) { participantId =>
      f(Institution.Id(participantId.uuid)).value
        .map {
          case Right(value) => value
          case Left(_) => throw new RuntimeException("Unknown error while retrieving groups")
        }
    }
  }

  override def updateGroup(request: console_api.UpdateGroupRequest): Future[console_api.UpdateGroupResponse] = {
    def f(issuerId: Institution.Id) = {
      issuerGroupsRepository
        .updateGroup(
          issuerId,
          IssuerGroup.Id.unsafeFrom(request.groupId),
          request.contactIdsToAdd.map(id => Contact.Id.unsafeFrom(id)).to(List),
          request.contactIdsToRemove.map(id => Contact.Id.unsafeFrom(id)).to(List)
        )
        .value
        .map {
          case Right(_) => console_api.UpdateGroupResponse()
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }

    authenticator.authenticated("getGroups", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

  override def copyGroup(request: CopyGroupRequest): Future[CopyGroupResponse] = ???

  override def deleteGroup(request: DeleteGroupRequest): Future[DeleteGroupResponse] = ???
}
