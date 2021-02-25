package io.iohk.atala.prism.management.console.models

import java.util.UUID
import enumeratum.{DoobieEnum, Enum, EnumEntry}
import io.iohk.atala.prism.models.UUIDValue

import java.time.Instant

final case class CreateCredentialTypeField(
    name: String,
    description: String
)

final case class CreateCredentialType(
    name: String,
    template: String,
    fields: List[CreateCredentialTypeField]
)

final case class UpdateCredentialType(
    id: CredentialTypeId,
    name: String,
    template: String,
    fields: List[CreateCredentialTypeField]
)

final case class CredentialType(
    id: CredentialTypeId,
    name: String,
    institution: ParticipantId,
    state: CredentialTypeState,
    template: String,
    createdAt: Instant
)

final case class CredentialTypeField(
    id: CredentialTypeFieldId,
    credentialTypeId: CredentialTypeId,
    name: String,
    description: String
)

case class CredentialTypeWithRequiredFields(credentialType: CredentialType, requiredFields: List[CredentialTypeField])

final case class CredentialTypeId(uuid: UUID) extends AnyVal with UUIDValue
object CredentialTypeId extends UUIDValue.Builder[CredentialTypeId]

final case class CredentialTypeFieldId(uuid: UUID) extends AnyVal with UUIDValue
object CredentialTypeFieldId extends UUIDValue.Builder[CredentialTypeFieldId]

sealed abstract class CredentialTypeState(value: String) extends EnumEntry {
  override def entryName: String = value
}
object CredentialTypeState extends Enum[CredentialTypeState] with DoobieEnum[CredentialTypeState] {
  lazy val values = findValues

  final case object Draft extends CredentialTypeState("DRAFT")
  final case object Ready extends CredentialTypeState("READY")
  final case object Archived extends CredentialTypeState("ARCHIVED")
}

final case class GetCredentialTypes()

final case class GetCredentialType(credentialTypeId: CredentialTypeId)

final case class MarkAsReadyCredentialType(credentialTypeId: CredentialTypeId)

final case class MarkAsArchivedCredentialType(credentialTypeId: CredentialTypeId)
