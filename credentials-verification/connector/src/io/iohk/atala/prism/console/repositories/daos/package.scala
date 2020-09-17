package io.iohk.atala.prism.console.repositories

import java.util.UUID

import doobie.util.Meta
import io.iohk.atala.prism.console.models.{Contact, Institution}
import io.iohk.atala.prism.daos.BaseDAO

package object daos extends BaseDAO {
  implicit val contactIdMeta: Meta[Contact.Id] = Meta[UUID].timap(Contact.Id.apply)(_.value)
  implicit val contactExternalIdMeta: Meta[Contact.ExternalId] = Meta[String].timap(Contact.ExternalId.apply)(_.value)
  implicit val contactConnectionStatusMeta: Meta[Contact.ConnectionStatus] =
    Meta[String].timap(Contact.ConnectionStatus.withNameInsensitive)(_.entryName)
  implicit val institutionIdMeta: Meta[Institution.Id] = Meta[UUID].timap(Institution.Id.apply)(_.value)
}
