package io.iohk.atala.prism.management.console.repositories

import java.util.UUID

import doobie.Meta
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.management.console.models.{Contact, ParticipantLogo}

package object daos extends BaseDAO {
  implicit val contactIdMeta: Meta[Contact.Id] = Meta[UUID].timap(Contact.Id.apply)(_.value)

  implicit val participantLogoMeta: Meta[ParticipantLogo] =
    Meta[Array[Byte]].timap(b => ParticipantLogo.apply(b.toVector))(_.bytes.toArray)
}
