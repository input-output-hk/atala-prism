package io.iohk.atala.prism.console.services

import io.iohk.atala.prism.connector.Authenticator
import io.iohk.atala.prism.console.models.{Institution, IssuerGroup}
import io.iohk.atala.prism.console.repositories.GroupsRepository
import io.iohk.prism.protos.{cmanager_api, cmanager_models}

import scala.concurrent.{ExecutionContext, Future}

class GroupsServiceImpl(issuerGroupsRepository: GroupsRepository, authenticator: Authenticator)(implicit
    ec: ExecutionContext
) extends cmanager_api.GroupsServiceGrpc.GroupsService {

  override def createGroup(request: cmanager_api.CreateGroupRequest): Future[cmanager_api.CreateGroupResponse] = {

    def f(issuerId: Institution.Id) = {
      issuerGroupsRepository
        .create(issuerId, IssuerGroup.Name(request.name))
        .value
        .map {
          case Right(_) => cmanager_api.CreateGroupResponse()
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }
    authenticator.authenticated("createGroup", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }

  }

  override def getGroups(request: cmanager_api.GetGroupsRequest): Future[cmanager_api.GetGroupsResponse] = {

    def f(issuerId: Institution.Id) = {
      issuerGroupsRepository
        .getBy(issuerId)
        .value
        .map {
          case Right(x) =>
            val groups = x.map(g => cmanager_models.Group(g.value))
            cmanager_api.GetGroupsResponse(groups)
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }

    authenticator.authenticated("getGroups", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

}
