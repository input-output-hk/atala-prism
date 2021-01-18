package io.iohk.atala.prism.management.console.repositories

import doobie.Meta
import doobie.postgres.implicits.pgEnumString
import doobie.util.Read
import doobie.util.invariant.InvalidEnum
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CredentialExternalId,
  CredentialIssuance,
  ParticipantLogo
}

import java.util.UUID

package object daos extends BaseDAO {
  implicit val contactIdMeta: Meta[Contact.Id] = Meta[UUID].timap(Contact.Id.apply)(_.value)

  implicit val participantLogoMeta: Meta[ParticipantLogo] =
    Meta[Array[Byte]].timap(b => ParticipantLogo.apply(b.toVector))(_.bytes.toArray)

  implicit val credentialExternalIdMeta: Meta[CredentialExternalId] =
    Meta[String].timap(CredentialExternalId.apply)(_.value)

  implicit val credentialIssuanceIdMeta: Meta[CredentialIssuance.Id] =
    Meta[UUID].timap(CredentialIssuance.Id.apply)(_.uuid)

  implicit val credentialIssuanceStatusMeta: Meta[CredentialIssuance.Status] = pgEnumString[CredentialIssuance.Status](
    "CREDENTIAL_ISSUANCE_STATUS_TYPE",
    a => CredentialIssuance.Status.withNameOption(a).getOrElse(throw InvalidEnum[CredentialIssuance.Status](a)),
    _.entryName
  )

  implicit val credentialIssuanceRead: Read[CredentialIssuance] =
    Read[(CredentialIssuance.Id, String, Int, CredentialIssuance.Status)].map[CredentialIssuance] {
      case (id, name, credentialTypeId, status) =>
        CredentialIssuance(id, name, credentialTypeId, status, contacts = List())
    }
}
