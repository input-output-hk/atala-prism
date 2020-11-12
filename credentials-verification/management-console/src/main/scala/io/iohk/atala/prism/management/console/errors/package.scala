package io.iohk.atala.prism.management.console

import io.grpc.Status
import io.iohk.atala.prism.errors.{PrismError, PrismServerError}
import io.iohk.atala.prism.management.console.models.{Contact, InstitutionGroup, ParticipantId}

package object errors {
  sealed trait ManagementConsoleError extends PrismError

  case class GroupDoesNotExist(groupId: InstitutionGroup.Id) extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(s"Group $groupId does not exist")
  }

  case class GroupInstitutionDoesNotMatch(groupInstitutionId: ParticipantId, participantInstitutionId: ParticipantId)
      extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Group belongs to institution $groupInstitutionId while $participantInstitutionId was provided"
      )
  }

  case class ContactsInstitutionsDoNotMatch(contactIds: List[Contact.Id], participantInstitutionId: ParticipantId)
      extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Contacts [${contactIds.mkString(", ")}] do not belong to institution $participantInstitutionId"
      )
  }

  case class UnknownValueError(tpe: String, value: String) extends ManagementConsoleError {
    override def toStatus: Status = {
      Status.UNKNOWN.withDescription(s"Unknown $tpe: $value")
    }
  }

  case class InternalServerError(cause: Throwable) extends ManagementConsoleError with PrismServerError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription("Internal server error. Please contact administrator.")
    }
  }

  def groupDoesNotExist[A](groupId: InstitutionGroup.Id): Either[ManagementConsoleError, A] =
    Left(GroupDoesNotExist(groupId))

  def groupInstitutionDoesNotMatch[A](
      groupInstitutionId: ParticipantId,
      participantInstitutionId: ParticipantId
  ): Either[ManagementConsoleError, A] =
    Left(GroupInstitutionDoesNotMatch(groupInstitutionId, participantInstitutionId))

  def contactsInstitutionsDoNotMatch[A](
      contactIds: List[Contact.Id],
      participantInstitutionId: ParticipantId
  ): Either[ManagementConsoleError, A] =
    Left(ContactsInstitutionsDoNotMatch(contactIds, participantInstitutionId))
}
