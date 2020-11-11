package io.iohk.atala.prism.console.services

import java.util.UUID

import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.console.models.{Contact, Institution, IssuerGroup}
import io.iohk.atala.prism.console.repositories.GroupsRepository
import io.iohk.atala.prism.protos.{cmanager_api, cmanager_models}
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

class GroupsServiceImpl(issuerGroupsRepository: GroupsRepository, authenticator: ConnectorAuthenticator)(implicit
    ec: ExecutionContext
) extends cmanager_api.GroupsServiceGrpc.GroupsService {

  override def createGroup(request: cmanager_api.CreateGroupRequest): Future[cmanager_api.CreateGroupResponse] = {

    def f(issuerId: Institution.Id) = {
      issuerGroupsRepository
        .create(issuerId, IssuerGroup.Name(request.name))
        .value
        .map {
          case Right(g) =>
            cmanager_api
              .CreateGroupResponse()
              .withGroup(
                cmanager_models
                  .Group()
                  .withId(g.id.value.toString)
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

  override def getGroups(request: cmanager_api.GetGroupsRequest): Future[cmanager_api.GetGroupsResponse] = {

    def f(issuerId: Institution.Id) = {
      lazy val contactIdT = if (request.contactId.nonEmpty) {
        Try(UUID.fromString(request.contactId))
          .orElse(Failure(new RuntimeException("The provided contactId is invalid")))
          .map(Contact.Id.apply)
          .map(Option.apply)
          .map(Right(_))
      } else Try(Right(None))

      for {
        contactIdMaybe <- Future.fromTry(contactIdT).toFutureEither
        groups <- issuerGroupsRepository.getBy(issuerId, contactIdMaybe)
      } yield {
        val proto = groups.map { g =>
          cmanager_models
            .Group()
            .withId(g.value.id.value.toString)
            .withCreatedAt(g.value.createdAt.getEpochSecond)
            .withName(g.value.name.value)
            .withNumberOfContacts(g.numberOfContacts)
        }
        cmanager_api.GetGroupsResponse(proto)
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

  override def updateGroup(request: cmanager_api.UpdateGroupRequest): Future[cmanager_api.UpdateGroupResponse] = {
    def f(issuerId: Institution.Id) = {
      issuerGroupsRepository
        .updateGroup(
          issuerId,
          IssuerGroup.Id(UUID.fromString(request.groupId)),
          request.contactIdsToAdd.map(id => Contact.Id(UUID.fromString(id))).to(List),
          request.contactIdsToRemove.map(id => Contact.Id(UUID.fromString(id))).to(List)
        )
        .value
        .map {
          case Right(_) => cmanager_api.UpdateGroupResponse()
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }

    authenticator.authenticated("getGroups", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }
}
