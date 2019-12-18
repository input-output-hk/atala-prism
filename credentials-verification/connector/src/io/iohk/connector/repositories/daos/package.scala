package io.iohk.connector.repositories

import java.util.UUID

import doobie.postgres.implicits._
import doobie.util.invariant.InvalidEnum
import doobie.util.{Get, Put}
import io.iohk.connector.model.payments.{ClientNonce, Payment}
import io.iohk.connector.model.{ConnectionId, MessageId, ParticipantLogo, ParticipantType}
import io.iohk.cvp.models.ParticipantId

package object daos {
  implicit val pgPackageTypeMeta = pgEnumString[ParticipantType](
    "PARTICIPANT_TYPE",
    a => ParticipantType.withNameOption(a).getOrElse(throw InvalidEnum[ParticipantType](a)),
    _.entryName
  )
  implicit val bigIntPut: Put[BigInt] = implicitly[Put[BigDecimal]].contramap(BigDecimal.apply)
  implicit val bigIntGet: Get[BigInt] = implicitly[Get[BigDecimal]].map(_.toBigInt())

  implicit val participantIdPut: Put[ParticipantId] = Put[UUID].contramap((_: ParticipantId).uuid)
  implicit val connectionIdPut: Put[ConnectionId] = Put[UUID].contramap((_: ConnectionId).id)
  implicit val messageIdPut: Put[MessageId] = Put[UUID].contramap((_: MessageId).id)

  implicit val participantIdGet: Get[ParticipantId] = Get[UUID].map(new ParticipantId(_))
  implicit val connectionIdGet: Get[ConnectionId] = Get[UUID].map(new ConnectionId(_))
  implicit val messageIdGet: Get[MessageId] = Get[UUID].map(new MessageId(_))

  implicit val participantLogoPut: Put[ParticipantLogo] = Put[Array[Byte]].contramap(_.bytes.toArray)
  implicit val participantLogoGet: Get[ParticipantLogo] =
    Get[Array[Byte]].map(bytes => ParticipantLogo.apply(bytes.toVector))

  implicit val paymentIdPut: Put[Payment.Id] = Put[UUID].contramap(_.uuid)
  implicit val paymentIdGet: Get[Payment.Id] = Get[UUID].map(Payment.Id.apply)

  implicit val clientNoncePut: Put[ClientNonce] = Put[String].contramap(_.string)
  implicit val clientNonceGet: Get[ClientNonce] = Get[String].map(s => new ClientNonce(s))

  implicit val paymentStatusPut: Put[Payment.Status] = Put[String].contramap(_.value)
  implicit val paymentStatusGet: Get[Payment.Status] = {
    Get[String].map(Payment.Status.withNameInsensitive)
  }
}
