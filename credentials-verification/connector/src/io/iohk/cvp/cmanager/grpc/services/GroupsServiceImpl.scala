package io.iohk.cvp.cmanager.grpc.services

import io.iohk.cvp.cmanager.models.IssuerGroup
import io.iohk.cvp.cmanager.protos
import io.iohk.cvp.cmanager.protos._
import io.iohk.cvp.cmanager.repositories.IssuerGroupsRepository

import scala.concurrent.{ExecutionContext, Future}

class GroupsServiceImpl(issuerGroupsRepository: IssuerGroupsRepository)(implicit ec: ExecutionContext)
    extends protos.GroupsServiceGrpc.GroupsService {

  override def createGroup(request: CreateGroupRequest): Future[CreateGroupResponse] = {
    val issuer = getIssuerId()
    issuerGroupsRepository
      .create(issuer, IssuerGroup.Name(request.name))
      .value
      .map {
        case Right(x) => CreateGroupResponse()
        case Left(e) => throw new RuntimeException(s"FAILED: $e")
      }
  }

  override def getGroups(request: GetGroupsRequest): Future[GetGroupsResponse] = {
    val issuer = getIssuerId()
    issuerGroupsRepository
      .getBy(issuer)
      .value
      .map {
        case Right(x) =>
          val groups = x.map(g => Group(g.value))
          GetGroupsResponse(groups)
        case Left(e) => throw new RuntimeException(s"FAILED: $e")
      }
  }
}
