package io.iohk.cvp.cmanager.grpc.services

import io.iohk.connector.Authenticator
import io.iohk.cvp.cmanager.models.{Issuer, IssuerGroup}
import io.iohk.cvp.cmanager.protos
import io.iohk.cvp.cmanager.protos._
import io.iohk.cvp.cmanager.repositories.IssuerGroupsRepository

import scala.concurrent.{ExecutionContext, Future}

class GroupsServiceImpl(issuerGroupsRepository: IssuerGroupsRepository, authenticator: Authenticator)(
    implicit ec: ExecutionContext
) extends protos.GroupsServiceGrpc.GroupsService {

  override def createGroup(request: CreateGroupRequest): Future[CreateGroupResponse] = {

    def f(issuerId: Issuer.Id) = {
      issuerGroupsRepository
        .create(issuerId, IssuerGroup.Name(request.name))
        .value
        .map {
          case Right(x) => CreateGroupResponse()
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }
    authenticator.authenticated("createGroup", request) { participantId =>
      f(Issuer.Id(participantId.uuid))
    }

  }

  override def getGroups(request: GetGroupsRequest): Future[GetGroupsResponse] = {

    def f(issuerId: Issuer.Id) = {
      issuerGroupsRepository
        .getBy(issuerId)
        .value
        .map {
          case Right(x) =>
            val groups = x.map(g => Group(g.value))
            GetGroupsResponse(groups)
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }

    authenticator.authenticated("getGroups", request) { participantId =>
      f(Issuer.Id(participantId.uuid))
    }
  }

}
