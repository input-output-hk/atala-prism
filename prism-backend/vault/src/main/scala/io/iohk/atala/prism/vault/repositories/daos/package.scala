package io.iohk.atala.prism.vault.repositories

import java.util.UUID

import doobie.Meta
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.vault.model.Payload

package object daos extends BaseDAO {
  implicit val payloadIdMeta: Meta[Payload.Id] = Meta[UUID].timap(Payload.Id.apply)(_.value)
  implicit val payloadExternalIdMeta: Meta[Payload.ExternalId] =
    Meta[UUID].timap(Payload.ExternalId.apply)(_.value)
}
