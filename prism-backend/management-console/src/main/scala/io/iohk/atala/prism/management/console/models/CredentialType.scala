package io.iohk.atala.prism.management.console.models

import java.util.UUID
import enumeratum.{DoobieEnum, Enum, EnumEntry}

import java.time.Instant

final case class CredentialType(
    id: CredentialTypeId,
    name: String,
    institution: Option[ParticipantId],
    state: CredentialTypeState,
    template: String,
    createdAt: Instant
) {
  val isShared: Boolean = institution.isEmpty
}

final case class CredentialTypeField(
    id: CredentialTypeFieldId,
    credentialTypeId: CredentialTypeId,
    name: String,
    description: String
)

case class CredentialTypeWithRequiredFields(credentialType: CredentialType, requiredFields: List[CredentialTypeField])

final case class CredentialTypeId(uuid: UUID) extends AnyVal

final case class CredentialTypeFieldId(uuid: UUID) extends AnyVal

sealed abstract class CredentialTypeState(value: String) extends EnumEntry {
  override def entryName: String = value
}
object CredentialTypeState extends Enum[CredentialTypeState] with DoobieEnum[CredentialTypeState] {
  lazy val values = findValues

  final case object Draft extends CredentialTypeState("DRAFT")
  final case object Ready extends CredentialTypeState("READY")
  final case object Archived extends CredentialTypeState("ARCHIVED")
}
