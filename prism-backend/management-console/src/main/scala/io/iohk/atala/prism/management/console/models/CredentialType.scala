package io.iohk.atala.prism.management.console.models

import derevo.derive

import java.util.UUID
import enumeratum.{DoobieEnum, Enum, EnumEntry}
import io.iohk.atala.prism.models.UUIDValue
import tofu.logging.derivation.loggable

import java.time.Instant

final case class CreateCredentialTypeField(
    name: String,
    description: String,
    `type`: CredentialTypeFieldType
)

final case class CreateCredentialType(
    name: String,
    template: String,
    fields: List[CreateCredentialTypeField],
    icon: Option[Vector[Byte]]
)

final case class UpdateCredentialType(
    id: CredentialTypeId,
    name: String,
    template: String,
    fields: List[CreateCredentialTypeField],
    icon: Option[Vector[Byte]]
)

final case class CredentialType(
    id: CredentialTypeId,
    name: String,
    institution: ParticipantId,
    state: CredentialTypeState,
    template: String,
    createdAt: Instant,
    icon: Option[Vector[Byte]]
)

final case class CredentialTypeField(
    id: CredentialTypeFieldId,
    credentialTypeId: CredentialTypeId,
    name: String,
    description: String,
    `type`: CredentialTypeFieldType
)

case class CredentialTypeWithRequiredFields(
    credentialType: CredentialType,
    requiredFields: List[CredentialTypeField]
)

@derive(loggable)
final case class CredentialTypeId(uuid: UUID) extends AnyVal with UUIDValue
object CredentialTypeId extends UUIDValue.Builder[CredentialTypeId]

final case class CredentialTypeFieldId(uuid: UUID) extends AnyVal with UUIDValue
object CredentialTypeFieldId extends UUIDValue.Builder[CredentialTypeFieldId]
@derive(loggable)
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

final case class MarkAsArchivedCredentialType(
    credentialTypeId: CredentialTypeId
)

sealed abstract class CredentialTypeFieldType(value: String) extends EnumEntry {
  override val entryName: String = value
}
object CredentialTypeFieldType extends Enum[CredentialTypeFieldType] with DoobieEnum[CredentialTypeFieldType] {
  lazy val values = findValues

  final case object String extends CredentialTypeFieldType("STRING")
  final case object Int extends CredentialTypeFieldType("INT")
  final case object Boolean extends CredentialTypeFieldType("BOOLEAN")
  final case object Date extends CredentialTypeFieldType("DATE")
}
