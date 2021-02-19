package io.iohk.atala.prism.management.console.models

import io.iohk.atala.prism.models.UUIDValue

import java.time.Instant
import java.util.UUID

final case class CreateInstitutionGroup(
    name: InstitutionGroup.Name,
    contactIds: Set[Contact.Id]
)

final case class UpdateInstitutionGroup(
    groupId: InstitutionGroup.Id,
    contactIdsToAdd: Set[Contact.Id],
    contactIdsToRemove: Set[Contact.Id],
    name: Option[InstitutionGroup.Name]
)

final case class DeleteInstitutionGroup(
    groupId: InstitutionGroup.Id
)

final case class InstitutionGroup(
    id: InstitutionGroup.Id,
    name: InstitutionGroup.Name,
    institutionId: ParticipantId,
    createdAt: Instant
)

object InstitutionGroup {
  final case class Id(uuid: UUID) extends AnyVal with UUIDValue
  object Id extends UUIDValue.Builder[Id]

  final case class Name(value: String) extends AnyVal
  final case class WithContactCount(value: InstitutionGroup, numberOfContacts: Int)

  object Name {
    def optional(string: String): Option[Name] = {
      if (string.trim.isEmpty) {
        None
      } else {
        Some(Name(string.trim))
      }
    }
  }
}
