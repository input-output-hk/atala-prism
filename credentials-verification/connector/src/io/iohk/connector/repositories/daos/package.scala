package io.iohk.connector.repositories

import java.util.UUID

import doobie.postgres.implicits._
import doobie.util.invariant.InvalidEnum
import doobie.util.{Put, Read}
import io.iohk.connector.model.{ConnectionId, MessageId, ParticipantId, ParticipantType}

package object daos {
  implicit val pgPackageTypeMeta = pgEnumString[ParticipantType](
    "PARTICIPANT_TYPE",
    a => ParticipantType.withNameOption(a).getOrElse(throw InvalidEnum[ParticipantType](a)),
    _.entryName
  )

  implicit val participantIdPut: Put[ParticipantId] = Put[UUID].contramap((_: ParticipantId).id)
  implicit val connectionIdPut: Put[ConnectionId] = Put[UUID].contramap((_: ConnectionId).id)
  implicit val messageIdPut: Put[MessageId] = Put[UUID].contramap((_: MessageId).id)

  implicit val participantIdRead: Read[ParticipantId] = Read[UUID].map(new ParticipantId(_))
  implicit val connectionIdRead: Read[ConnectionId] = Read[UUID].map(new ConnectionId(_))
  implicit val messageIdRead: Read[MessageId] = Read[UUID].map(new MessageId(_))

}
