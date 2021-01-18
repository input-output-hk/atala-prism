package io.iohk.atala.prism.management.console.models

import enumeratum.{Enum, EnumEntry}
import io.circe.Json

import java.util.UUID

/** Represents the metadata to issue credentials to a batch of contacts. */
case class CredentialIssuance(
    id: CredentialIssuance.Id,
    name: String,
    credentialTypeId: Int,
    status: CredentialIssuance.Status,
    contacts: List[CredentialIssuanceContact]
)

case class CredentialIssuanceContactGroup(id: CredentialIssuance.ContactGroupId, groupId: InstitutionGroup.Id)

case class CredentialIssuanceContact(
    id: CredentialIssuance.ContactId,
    contactId: Contact.Id,
    credentialData: Json
)

object CredentialIssuance {
  final case class Id(uuid: UUID) extends AnyVal
  final case class ContactId(uuid: UUID) extends AnyVal
  final case class ContactGroupId(uuid: UUID) extends AnyVal

  sealed abstract class Status(val value: String) extends EnumEntry {
    override def entryName: String = value
  }
  object Status extends Enum[Status] {
    lazy val values: IndexedSeq[Status] = findValues

    final case object Draft extends Status("DRAFT")
    final case object Ready extends Status("READY")
    final case object Completed extends Status("COMPLETED")
  }
}
