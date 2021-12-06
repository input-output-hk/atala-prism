package io.iohk.atala.prism.vault.repositories

import doobie.Meta
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.vault.model.Record

package object daos extends BaseDAO {
  implicit val recordTypeMeta: Meta[Record.Type] = Meta[Array[Byte]]
    .timap(Record.Type.unsafeFrom)(_.encrypted.toArray)

  implicit val recordIdMeta: Meta[Record.Id] = Meta[Array[Byte]]
    .timap(Record.Id.unsafeFrom)(_.encrypted.toArray)

  implicit val recordPayloadMeta: Meta[Record.Payload] =
    Meta[Array[Byte]].timap(Record.Payload.unsafeFrom)(_.encrypted.toArray)
}
