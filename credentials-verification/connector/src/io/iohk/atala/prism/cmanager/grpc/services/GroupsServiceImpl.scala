package io.iohk.atala.prism.cmanager.grpc.services

import io.iohk.atala.prism.connector.Authenticator
import io.iohk.atala.prism.cmanager.models.{Issuer, IssuerGroup}
import io.iohk.atala.prism.cmanager.repositories.IssuerGroupsRepository
import io.iohk.prism.protos.{cmanager_api, cmanager_models}

import scala.concurrent.{ExecutionContext, Future}

class GroupsServiceImpl(issuerGroupsRepository: IssuerGroupsRepository, authenticator: Authenticator)(implicit
    ec: ExecutionContext
) extends cmanager_api.GroupsServiceGrpc.GroupsService {

  override def createGroup(request: cmanager_api.CreateGroupRequest): Future[cmanager_api.CreateGroupResponse] = {

    def f(issuerId: Issuer.Id) = {
      issuerGroupsRepository
        .create(issuerId, IssuerGroup.Name(request.name))
        .value
        .map {
          case Right(_) => cmanager_api.CreateGroupResponse()
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }
    authenticator.authenticated("createGroup", request) { participantId =>
      f(Issuer.Id(participantId.uuid))
    }

  }

  override def getGroups(request: cmanager_api.GetGroupsRequest): Future[cmanager_api.GetGroupsResponse] = {

    def f(issuerId: Issuer.Id) = {
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
      f(Issuer.Id(participantId.uuid))
    }
  }

}
