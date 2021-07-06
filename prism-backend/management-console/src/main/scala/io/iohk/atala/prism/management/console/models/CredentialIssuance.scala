package io.iohk.atala.prism.management.console.models

import io.circe.Json
import io.iohk.atala.prism.models.UUIDValue

import java.time.Instant
import java.util.UUID

/** Represents the metadata to issue credentials to a batch of contacts. */
case class CredentialIssuance(
    id: CredentialIssuance.Id,
    name: String,
    credentialTypeId: CredentialTypeId,
    createdAt: Instant,
    contacts: List[CredentialIssuanceContact]
)

case class CredentialIssuanceContactGroup(id: CredentialIssuance.ContactGroupId, groupId: InstitutionGroup.Id)

case class CredentialIssuanceContact(
    id: CredentialIssuance.ContactId,
    contactId: Contact.Id,
    credentialData: Json,
    groupIds: List[InstitutionGroup.Id]
)

object CredentialIssuance {
  final case class Id(uuid: UUID) extends AnyVal with UUIDValue
  object Id extends UUIDValue.Builder[Id]

  final case class ContactId(uuid: UUID) extends AnyVal with UUIDValue
  object ContactId extends UUIDValue.Builder[ContactId]

  final case class ContactGroupId(uuid: UUID) extends AnyVal with UUIDValue
  object ContactGroupId extends UUIDValue.Builder[ContactGroupId]
}
