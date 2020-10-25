package io.iohk.atala.prism.vault.repositories

import java.util.UUID

import doobie.Meta
import io.iohk.atala.prism.vault.model.Payload

package object daos {
  implicit val uuidMeta: Meta[UUID] = doobie.postgres.implicits.UuidType
  implicit val payloadIdMeta: Meta[Payload.Id] = Meta[UUID].timap(Payload.Id.apply)(_.value)
}
