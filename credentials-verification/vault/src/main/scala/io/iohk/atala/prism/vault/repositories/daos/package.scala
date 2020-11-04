package io.iohk.atala.prism.vault.repositories

import java.util.UUID

import doobie.Meta
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.vault.model.Payload

package object daos {
  implicit val sha256Meta: Meta[SHA256Digest] = Meta[String].timap(SHA256Digest.fromHex)(_.hexValue)
  implicit val uuidMeta: Meta[UUID] = doobie.postgres.implicits.UuidType
  implicit val payloadIdMeta: Meta[Payload.Id] = Meta[UUID].timap(Payload.Id.apply)(_.value)
  implicit val payloadExternalIdMeta: Meta[Payload.ExternalId] =
    Meta[UUID].timap(Payload.ExternalId.apply)(_.value)
}
