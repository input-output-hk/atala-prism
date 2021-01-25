package io.iohk.atala.prism.management.console.repositories

import doobie.Meta
import doobie.implicits.legacy.instant._
import doobie.postgres.implicits.pgEnumString
import doobie.util.Read
import doobie.util.invariant.InvalidEnum
import io.circe.Json
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CredentialExternalId,
  CredentialIssuance,
  CredentialIssuanceContact,
  ParticipantLogo
}

import java.time.Instant

package object daos extends BaseDAO {
  implicit val contactIdMeta: Meta[Contact.Id] = uuidValueMeta(Contact.Id)

  implicit val participantLogoMeta: Meta[ParticipantLogo] =
    Meta[Array[Byte]].timap(b => ParticipantLogo.apply(b.toVector))(_.bytes.toArray)

  implicit val credentialExternalIdMeta: Meta[CredentialExternalId] =
    Meta[String].timap(CredentialExternalId.apply)(_.value)

  implicit val credentialIssuanceIdMeta: Meta[CredentialIssuance.Id] = uuidValueMeta(CredentialIssuance.Id)

  implicit val credentialIssuanceStatusMeta: Meta[CredentialIssuance.Status] = pgEnumString[CredentialIssuance.Status](
    "CREDENTIAL_ISSUANCE_STATUS_TYPE",
    a => CredentialIssuance.Status.withNameOption(a).getOrElse(throw InvalidEnum[CredentialIssuance.Status](a)),
    _.entryName
  )

  implicit val credentialIssuanceRead: Read[CredentialIssuance] =
    Read[(CredentialIssuance.Id, String, Int, CredentialIssuance.Status, Instant)].map[CredentialIssuance] {
      case (id, name, credentialTypeId, status, createdAt) =>
        CredentialIssuance(id, name, credentialTypeId, status, createdAt, contacts = List())
    }

  implicit val credentialIssuanceContactRead: Read[CredentialIssuanceContact] =
    Read[(CredentialIssuance.ContactId, Contact.Id, Json)].map[CredentialIssuanceContact] {
      case (id, contactId, credentialData) =>
        CredentialIssuanceContact(id, contactId, credentialData, groupIds = List())
    }
}
