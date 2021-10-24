package io.iohk.atala.prism.management.console.models

import derevo.derive
import io.iohk.atala.prism.models.UUIDValue
import tofu.logging.derivation.loggable

import java.time.{Instant, LocalDate}
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

final case class CopyInstitutionGroup(
    groupId: InstitutionGroup.Id,
    newName: InstitutionGroup.Name
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
  @derive(loggable)
  final case class Id(uuid: UUID) extends AnyVal with UUIDValue
  object Id extends UUIDValue.Builder[Id]
  @derive(loggable)
  final case class Name(value: String) extends AnyVal
  final case class WithContactCount(
      value: InstitutionGroup,
      numberOfContacts: Int
  )

  object Name {
    def optional(string: String): Option[Name] = {
      if (string.trim.isEmpty) {
        None
      } else {
        Some(Name(string.trim))
      }
    }
  }

  // Used to sort the results by the given field
  sealed trait SortBy extends Product with Serializable
  object SortBy {
    final case object Name extends SortBy
    final case object CreatedAt extends SortBy
    final case object NumberOfContacts extends SortBy
  }

  case class FilterBy(
      name: Option[InstitutionGroup.Name] = None,
      createdAfter: Option[LocalDate] = None,
      createdBefore: Option[LocalDate] = None,
      contactId: Option[Contact.Id] = None
  )

  type PaginatedQuery =
    PaginatedQueryConstraints[
      InstitutionGroup.Id,
      InstitutionGroup.SortBy,
      InstitutionGroup.FilterBy
    ]

}
