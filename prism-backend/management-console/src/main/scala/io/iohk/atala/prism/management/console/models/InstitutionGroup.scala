package io.iohk.atala.prism.management.console.models

import java.time.Instant
import java.util.UUID

final case class InstitutionGroup(
    id: InstitutionGroup.Id,
    name: InstitutionGroup.Name,
    institutionId: ParticipantId,
    createdAt: Instant
)

object InstitutionGroup {
  final case class Id(value: UUID) extends AnyVal
  final case class Name(value: String) extends AnyVal
  final case class WithContactCount(value: InstitutionGroup, numberOfContacts: Int)
}
