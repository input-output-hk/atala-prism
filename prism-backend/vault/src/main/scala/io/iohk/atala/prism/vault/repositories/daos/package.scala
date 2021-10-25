package io.iohk.atala.prism.vault.repositories

import doobie.Meta
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.vault.model.Payload

package object daos extends BaseDAO {
  implicit val payloadIdMeta: Meta[Payload.Id] = uuidValueMeta(Payload.Id)
  implicit val payloadExternalIdMeta: Meta[Payload.ExternalId] = uuidValueMeta(
    Payload.ExternalId.apply
  )
}
