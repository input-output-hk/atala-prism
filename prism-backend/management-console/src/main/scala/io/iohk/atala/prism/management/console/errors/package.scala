package io.iohk.atala.prism.management.console

import cats.syntax.semigroup._
import derevo.derive
import io.circe.Json
import io.grpc.Status
import io.iohk.atala.prism.errors.{PrismError, PrismServerError}
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CredentialTypeId,
  CredentialTypeState,
  GenericCredential,
  InstitutionGroup,
  ParticipantId
}
import io.iohk.atala.prism.management.console.validations.CredentialDataValidationError
import tofu.logging.{DictLoggable, LogRenderer, Loggable}
import tofu.logging.derivation.loggable

package object errors {

  @derive(loggable)
  sealed trait ManagementConsoleError extends PrismError

  case class GroupDoesNotExist(groupId: InstitutionGroup.Id) extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(s"Group $groupId does not exist")
  }

  case class GroupInstitutionDoesNotMatch(
      groupInstitutionId: ParticipantId,
      participantInstitutionId: ParticipantId
  ) extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Group belongs to institution $groupInstitutionId while $participantInstitutionId was provided"
      )
  }

  case class ContactsInstitutionsDoNotMatch(
      contactIds: List[Contact.Id],
      participantInstitutionId: ParticipantId
  ) extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Contacts [${contactIds.mkString(", ")}] do not belong to institution $participantInstitutionId"
      )
  }

  case class GroupsInstitutionDoNotMatch(
      groupIds: List[InstitutionGroup.Id],
      participantInstitutionId: ParticipantId
  ) extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Groups [${groupIds.mkString(", ")}] do not belong to institution $participantInstitutionId"
      )
  }

  case class GenerationOfConnectionTokensFailed(
      expectedTokenCount: Int,
      actualTokenCount: Int
  ) extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Generation of Connection Tokens failed, expected token count: $expectedTokenCount " +
          s"but connector generated: $actualTokenCount"
      )
  }

  case class PublishedCredentialsNotExist(
      nonExistingCredentialIds: List[GenericCredential.Id]
  ) extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Credentials with following ids don't exist or has not been " +
          s"published yet: ${nonExistingCredentialIds.map(_.uuid.toString).mkString(", ")}"
      )
  }

  case class PublishedCredentialsNotRevoked(
      credentialsIds: List[GenericCredential.Id]
  ) extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Cannot delete published, not revoked credentials: " +
          s"${credentialsIds.map(_.uuid.toString).mkString(", ")}"
      )
  }

  case class UnknownValueError(tpe: String, value: String) extends ManagementConsoleError {
    override def toStatus: Status = {
      Status.UNKNOWN.withDescription(s"Unknown $tpe: $value")
    }
  }

  final case class InvalidRequest(reason: String) extends ManagementConsoleError {
    def toStatus: Status = Status.INVALID_ARGUMENT.withDescription(reason)
  }

  case class InternalServerError(cause: Throwable) extends ManagementConsoleError with PrismServerError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription(
        "Internal server error. Please contact administrator."
      )
    }
  }

  case class CredentialTypeDoesNotExist(credentialTypeId: CredentialTypeId) extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Credential type with id: ${credentialTypeId} does not exist"
      )
  }

  case class CredentialTypeDoesNotBelongToInstitution(
      credentialTypeId: CredentialTypeId,
      institutionId: ParticipantId
  ) extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Credential type with id: $credentialTypeId does not belong to institution: $institutionId"
      )
  }

  case class CredentialTypeUpdateIncorrectState(
      credentialTypeId: CredentialTypeId,
      name: String,
      credentialTypeState: CredentialTypeState
  ) extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Credential type with id: $credentialTypeId and name: $name " +
          s"cannot be updated in ${credentialTypeState} state, updates are only allowed in DRAFT state"
      )
  }

  case class CredentialTypeMarkArchivedAsReady(
      credentialTypeId: CredentialTypeId
  ) extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Credential type with id: ${credentialTypeId} cannot be marked as READY because it is currently " +
          s"marked as ARCHIVED. Only credential types in DRAFT state can be moved to READY state"
      )
  }

  case class CredentialTypeIncorrectMustacheTemplate(
      name: String,
      templateError: String
  ) extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Credential type with name: $name " +
          s"has incorrect mustache template: $templateError"
      )
  }

  case class CredentialDataValidationFailedForContacts(
      credentialTypeName: String,
      contacts: List[(Contact.Id, Json, List[CredentialDataValidationError])]
  ) extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Credential type: $credentialTypeName can not be rendered " +
          s"with contacts credential data \n" + contacts
            .map { case (contactId, credentialData, errors) =>
              s"Contact: $contactId credential data: $credentialData errors:\n ${errors.map(_.message).mkString(",\n")} \n"
            }
            .mkString("\n")
      )
  }

  object CredentialDataValidationFailedForContacts {
    implicit val loggable: Loggable[CredentialDataValidationFailedForContacts] =
      new DictLoggable[CredentialDataValidationFailedForContacts] {
        override def fields[I, V, R, S](
            a: CredentialDataValidationFailedForContacts,
            i: I
        )(implicit
            r: LogRenderer[I, V, R, S]
        ): R =
          r.addString("credentialTypeName", a.credentialTypeName, i) |+| r
            .addString(
              "errors",
              a.contacts.map(_._1).mkString(","),
              i
            )

        override def logShow(
            a: CredentialDataValidationFailedForContacts
        ): String =
          s"CredentialDataValidationFailedForContacts{credentialTypeName=${a.credentialTypeName},contacts=${showContacts(a.contacts)}}"
      }

    private def showContacts(
        in: List[(Contact.Id, Json, List[CredentialDataValidationError])]
    ): String =
      in.map(contact => s"contactId=${contact._1}, errors=${showValidationErrorsMessages(contact._3)}").mkString(",")

    private def showValidationErrorsMessages(
        in: List[CredentialDataValidationError]
    ): String =
      in.map(_.message).mkString(",")
  }

  case class CredentialDataValidationFailed(
      credentialTypeName: String,
      credentialData: Json,
      errors: List[CredentialDataValidationError]
  ) extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Credential type: $credentialTypeName can not be rendered with credential data $credentialData " +
          s"errors: ${errors.map(_.message).mkString("\n")}"
      )
  }

  object CredentialDataValidationFailed {
    implicit val loggable: Loggable[CredentialDataValidationFailed] =
      new DictLoggable[CredentialDataValidationFailed] {
        override def fields[I, V, R, S](
            a: CredentialDataValidationFailed,
            i: I
        )(implicit r: LogRenderer[I, V, R, S]): R =
          r.addString("credentialTypeName", a.credentialTypeName, i) |+| r
            .addString(
              "errors",
              a.errors.map(_.message).mkString(","),
              i
            )

        override def logShow(a: CredentialDataValidationFailed): String =
          s"CredentialDataValidationFailed{credentialTypeName=${a.credentialTypeName},errors=${a.errors}}"
      }
  }

  case class GetContactsInvalidRequest(reason: String) extends ManagementConsoleError {
    def toStatus: Status = Status.INVALID_ARGUMENT.withDescription(reason)
  }

  case class CreateContactsInvalidRequest(reason: String) extends ManagementConsoleError {
    def toStatus: Status = Status.INVALID_ARGUMENT.withDescription(reason)
  }

  case class UpdateContactInvalidRequest(reason: String) extends ManagementConsoleError {
    def toStatus: Status = Status.INVALID_ARGUMENT.withDescription(reason)
  }

  case class ContactHasExistingCredentials(contactId: Contact.Id) extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Contact with id '${contactId.uuid}' has some existing credentials"
      )
  }

  case class GroupNameIsNotFree(name: InstitutionGroup.Name) extends ManagementConsoleError {
    def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Group with name '${name.value}' already exists"
      )
  }

  case class UpdateGroupInvalidRequest(reason: String) extends ManagementConsoleError {
    def toStatus: Status = Status.INVALID_ARGUMENT.withDescription(reason)
  }

  case class GetStatisticsInvalidRequest(reason: String) extends ManagementConsoleError {
    def toStatus: Status = Status.INVALID_ARGUMENT.withDescription(reason)
  }

  case class DeleteGroupInvalidRequest(reason: String) extends ManagementConsoleError {
    def toStatus: Status = Status.INVALID_ARGUMENT.withDescription(reason)
  }

  case class ContactIdsWereNotFound(contactIds: Set[Contact.Id]) extends ManagementConsoleError {
    override def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Contacts with id [${contactIds.map(_.uuid).mkString(", ")}] do not exist"
      )
  }

  case class ExternalIdsWereNotFound(externalIds: Set[Contact.ExternalId]) extends ManagementConsoleError {
    override def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Contacts with external id [${externalIds.map(_.value).mkString(", ")}] do not exist"
      )
  }

  case class InvalidGroups(groupIds: Set[InstitutionGroup.Id]) extends ManagementConsoleError {
    override def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        s"Groups [${groupIds.map(_.uuid).mkString(", ")}] are invalid"
      )
  }

  case object MissingContactIdAndExternalId extends ManagementConsoleError {
    override def toStatus: Status =
      Status.INVALID_ARGUMENT.withDescription(
        "Both contact id and external id are missing, one is required"
      )
  }

  def groupDoesNotExist[A](
      groupId: InstitutionGroup.Id
  ): Either[ManagementConsoleError, A] =
    Left(GroupDoesNotExist(groupId))

  def groupInstitutionDoesNotMatch[A](
      groupInstitutionId: ParticipantId,
      participantInstitutionId: ParticipantId
  ): Either[ManagementConsoleError, A] =
    Left(
      GroupInstitutionDoesNotMatch(groupInstitutionId, participantInstitutionId)
    )

  def contactsInstitutionsDoNotMatch[A](
      contactIds: List[Contact.Id],
      participantInstitutionId: ParticipantId
  ): Either[ManagementConsoleError, A] =
    Left(ContactsInstitutionsDoNotMatch(contactIds, participantInstitutionId))
}
