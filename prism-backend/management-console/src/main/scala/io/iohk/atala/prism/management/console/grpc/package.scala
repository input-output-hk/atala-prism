package io.iohk.atala.prism.management.console

import cats.syntax.traverse._
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.management.console.grpc.ProtoCodecs.{checkListUniqueness, toTimestamp}
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CreateInstitutionGroup,
  DeleteInstitutionGroup,
  GetInstitutionGroups,
  GetStatistics,
  InstitutionGroup,
  UpdateInstitutionGroup
}
import io.iohk.atala.prism.protos.console_api.{
  CreateGroupRequest,
  DeleteGroupRequest,
  GetGroupsRequest,
  GetStatisticsRequest,
  UpdateGroupRequest
}

import scala.util.Success

package object grpc {
  implicit val getStatisticsConverter: ProtoConverter[GetStatisticsRequest, GetStatistics] =
    (request: GetStatisticsRequest) => {
      request.interval match {
        case Some(protoInterval) =>
          toTimestamp(protoInterval).map(timeInterval => GetStatistics(Some(timeInterval)))
        case None =>
          Success(GetStatistics(None))
      }
    }

  implicit val createGroupConverter: ProtoConverter[CreateGroupRequest, CreateInstitutionGroup] =
    (request: CreateGroupRequest) => {
      for {
        contactIds <- request.contactIds.toList.map(Contact.Id.from).sequence
        contactIdsSet <- checkListUniqueness(contactIds)
        name = InstitutionGroup.Name(request.name)
      } yield CreateInstitutionGroup(name, contactIdsSet)
    }

  implicit val getGroupsConverter: ProtoConverter[GetGroupsRequest, GetInstitutionGroups] =
    (request: GetGroupsRequest) => {
      if (request.contactId.nonEmpty) {
        Contact.Id.from(request.contactId).map(cId => GetInstitutionGroups(Some(cId)))
      } else {
        Success(GetInstitutionGroups(None))
      }
    }

  implicit val updateGroupConverter: ProtoConverter[UpdateGroupRequest, UpdateInstitutionGroup] =
    (request: UpdateGroupRequest) => {
      for {
        groupId <- InstitutionGroup.Id.from(request.groupId)
        contactIdsToAdd <- request.contactIdsToAdd.toList.map(Contact.Id.from).sequence
        contactIdsToRemove <- request.contactIdsToRemove.toList.map(Contact.Id.from).sequence
        contactIdsToAddSet <- checkListUniqueness(contactIdsToAdd)
        contactIdsToRemoveSet <- checkListUniqueness(contactIdsToRemove)
        name = if (request.name.isEmpty) None else Some(InstitutionGroup.Name(request.name))
      } yield UpdateInstitutionGroup(groupId, contactIdsToAddSet, contactIdsToRemoveSet, name)
    }

  implicit val deleteGroupConverter: ProtoConverter[DeleteGroupRequest, DeleteInstitutionGroup] =
    (request: DeleteGroupRequest) => {
      InstitutionGroup.Id.from(request.groupId).map(DeleteInstitutionGroup)
    }
}
