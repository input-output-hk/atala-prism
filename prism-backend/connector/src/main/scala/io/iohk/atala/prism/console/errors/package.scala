package io.iohk.atala.prism.console

import io.grpc.Status
import io.iohk.atala.prism.console.models.{Contact, Institution, IssuerGroup}

package object errors {
  sealed trait ConsoleError {
    def toStatus: Status
  }

  case class GroupDoesNotExist(groupId: IssuerGroup.Id) extends ConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(s"Group $groupId does not exist")
  }

  case class GroupIssuerDoesNotMatch(groupIssuerId: Institution.Id, participantIssuerId: Institution.Id)
      extends ConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Group belongs to institution $groupIssuerId while $participantIssuerId was provided"
      )
  }

  case class ContactsIssuersDoNotMatch(contactIds: List[Contact.Id], participantIssuerId: Institution.Id)
      extends ConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Contacts [${contactIds.mkString(", ")}] do not belong to institution $participantIssuerId"
      )
  }

  def groupDoesNotExist[A](groupId: IssuerGroup.Id): Either[ConsoleError, A] =
    Left(GroupDoesNotExist(groupId))

  def groupIssuerDoesNotMatch[A](
      groupIssuerId: Institution.Id,
      participantIssuerId: Institution.Id
  ): Either[ConsoleError, A] =
    Left(GroupIssuerDoesNotMatch(groupIssuerId, participantIssuerId))

  def contactsIssuersDoNotMatch[A](
      contactIds: List[Contact.Id],
      participantIssuerId: Institution.Id
  ): Either[ConsoleError, A] =
    Left(ContactsIssuersDoNotMatch(contactIds, participantIssuerId))
}
