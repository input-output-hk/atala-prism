package io.iohk.atala.prism.connector.repositories

import doobie.postgres.implicits._
import doobie.Meta
import doobie.util.invariant.InvalidEnum
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.connector.model.{ConnectionStatus, MessageId, ParticipantLogo, ParticipantType}

package object daos extends BaseDAO {

  implicit val pgPackageTypeMeta: Meta[ParticipantType] =
    pgEnumString[ParticipantType](
      "PARTICIPANT_TYPE",
      a =>
        ParticipantType
          .withNameOption(a)
          .getOrElse(throw InvalidEnum[ParticipantType](a)),
      _.entryName
    )

  implicit val messageIdMeta: Meta[MessageId] =
    uuidMeta.timap(MessageId.apply)(_.uuid)

  implicit val participantLogoMeta: Meta[ParticipantLogo] =
    Meta[Array[Byte]].timap(b => ParticipantLogo.apply(b.toVector))(
      _.bytes.toArray
    )

  implicit val contactConnectionStatusMeta: Meta[ConnectionStatus] =
    Meta[String].timap(ConnectionStatus.withNameInsensitive)(_.entryName)
}
