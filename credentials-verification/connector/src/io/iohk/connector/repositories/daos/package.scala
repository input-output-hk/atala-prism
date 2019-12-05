package io.iohk.connector.repositories

import java.util.UUID

import doobie.postgres.implicits._
import doobie.util.invariant.InvalidEnum
import doobie.util.{Put, Read}
import io.iohk.connector.model.{ConnectionId, MessageId, ParticipantLogo, ParticipantType}
import io.iohk.cvp.models.ParticipantId

package object daos {
  implicit val pgPackageTypeMeta = pgEnumString[ParticipantType](
    "PARTICIPANT_TYPE",
    a => ParticipantType.withNameOption(a).getOrElse(throw InvalidEnum[ParticipantType](a)),
    _.entryName
  )
  implicit val bigIntPut: Put[BigInt] = implicitly[Put[BigDecimal]].contramap(BigDecimal.apply)
  implicit val bigIntRead: Read[BigInt] = implicitly[Read[BigDecimal]].map(_.toBigInt())

  implicit val participantIdPut: Put[ParticipantId] = Put[UUID].contramap((_: ParticipantId).uuid)
  implicit val connectionIdPut: Put[ConnectionId] = Put[UUID].contramap((_: ConnectionId).id)
  implicit val messageIdPut: Put[MessageId] = Put[UUID].contramap((_: MessageId).id)

  implicit val participantIdRead: Read[ParticipantId] = Read[UUID].map(new ParticipantId(_))
  implicit val connectionIdRead: Read[ConnectionId] = Read[UUID].map(new ConnectionId(_))
  implicit val messageIdRead: Read[MessageId] = Read[UUID].map(new MessageId(_))

  implicit val participantLogoPut: Put[ParticipantLogo] = Put[Array[Byte]].contramap(_.bytes.toArray)
  implicit val participantLogoRead: Read[ParticipantLogo] =
    Read[Array[Byte]].map(bytes => ParticipantLogo.apply(bytes.toVector))

}
