package io.iohk.atala.prism.management.console.repositories

import java.util.UUID

import doobie.Meta
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.management.console.models.Contact

package object daos extends BaseDAO {
  implicit val contactIdMeta: Meta[Contact.Id] = Meta[UUID].timap(Contact.Id.apply)(_.value)
}
