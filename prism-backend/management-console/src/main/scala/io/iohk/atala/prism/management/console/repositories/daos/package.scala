package io.iohk.atala.prism.management.console.repositories

import doobie.Meta
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CredentialExternalId,
  CredentialIssuance,
  ParticipantLogo
}

package object daos extends BaseDAO {
  implicit val contactIdMeta: Meta[Contact.Id] = uuidValueMeta(Contact.Id)

  implicit val participantLogoMeta: Meta[ParticipantLogo] =
    Meta[Array[Byte]].timap(b => ParticipantLogo.apply(b.toVector))(_.bytes.toArray)

  implicit val credentialExternalIdMeta: Meta[CredentialExternalId] =
    Meta[String].timap(CredentialExternalId.apply)(_.value)

  implicit val credentialIssuanceIdMeta: Meta[CredentialIssuance.Id] = uuidValueMeta(CredentialIssuance.Id)
}
