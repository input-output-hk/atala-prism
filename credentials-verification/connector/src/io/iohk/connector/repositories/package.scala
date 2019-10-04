package io.iohk.connector

import java.util.UUID

import doobie.postgres.implicits._
import doobie.util.{Put, Read}
import doobie.util.invariant.InvalidEnum
import io.iohk.connector.model.{ConnectionId, ParticipantId, ParticipantType}

package object repositories {

  implicit val pgPackageTypeMeta = pgEnumString[ParticipantType](
    "PARTICIPANT_TYPE",
    a => ParticipantType.withNameOption(a).getOrElse(throw InvalidEnum[ParticipantType](a)),
    _.entryName
  )

  implicit val participantIdPut: Put[ParticipantId] = Put[UUID].contramap((_: ParticipantId).id)
  implicit val connectionIdPut: Put[ConnectionId] = Put[UUID].contramap((_: ConnectionId).id)

  implicit val participantIdRead: Read[ParticipantId] = Read[UUID].map(new ParticipantId(_))
  implicit val connectionIdRead: Read[ConnectionId] = Read[UUID].map(new ConnectionId(_))
}
