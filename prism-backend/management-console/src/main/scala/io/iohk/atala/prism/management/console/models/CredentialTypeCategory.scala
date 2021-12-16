package io.iohk.atala.prism.management.console.models

import derevo.derive

import java.util.UUID
import enumeratum.{DoobieEnum, Enum, EnumEntry}
import io.iohk.atala.prism.models.UUIDValue
import tofu.logging.derivation.loggable

final case class CreateCredentialTypeCategory(
    name: String,
    state: CredentialTypeCategoryState
)

final case class GetCredentialTypeCategories()

final case class ArchiveCredentialTypeCategory(
    id: CredentialTypeCategoryId
)

final case class UnArchiveCredentialTypeCategory(
    id: CredentialTypeCategoryId
)

final case class CredentialTypeCategory(
    id: CredentialTypeCategoryId,
    name: String,
    institutionId: ParticipantId,
    state: CredentialTypeCategoryState
)

@derive(loggable)
final case class CredentialTypeCategoryId(uuid: UUID) extends AnyVal with UUIDValue
object CredentialTypeCategoryId extends UUIDValue.Builder[CredentialTypeCategoryId]

@derive(loggable)
sealed abstract class CredentialTypeCategoryState(value: String) extends EnumEntry {
  override def entryName: String = value
}
object CredentialTypeCategoryState
    extends Enum[CredentialTypeCategoryState]
    with DoobieEnum[CredentialTypeCategoryState] {
  lazy val values = findValues

  final case object Draft extends CredentialTypeCategoryState("DRAFT")
  final case object Ready extends CredentialTypeCategoryState("READY")
  final case object Archived extends CredentialTypeCategoryState("ARCHIVED")
}
