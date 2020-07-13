package io.iohk.connector.repositories

import doobie.postgres.implicits._
import doobie.util.Meta
import doobie.util.invariant.InvalidEnum
import io.iohk.atala.crypto.{EC, ECPublicKey}
import io.iohk.connector.model.payments.{ClientNonce, Payment}
import io.iohk.connector.model.{MessageId, ParticipantLogo, ParticipantType}
import io.iohk.cvp.daos.BaseDAO

package object daos extends BaseDAO {

  implicit val pgPackageTypeMeta: Meta[ParticipantType] = pgEnumString[ParticipantType](
    "PARTICIPANT_TYPE",
    a => ParticipantType.withNameOption(a).getOrElse(throw InvalidEnum[ParticipantType](a)),
    _.entryName
  )

  implicit val messageIdMeta: Meta[MessageId] = uuidMeta.timap(MessageId.apply)(_.id)

  implicit val participantLogoMeta: Meta[ParticipantLogo] =
    Meta[Array[Byte]].timap(b => ParticipantLogo.apply(b.toVector))(_.bytes.toArray)

  implicit val ecPublicKeyMeta: Meta[ECPublicKey] =
    Meta[Array[Byte]].timap(b => EC.toPublicKey(b))(_.getEncoded)

  implicit val paymentIdMeta: Meta[Payment.Id] = uuidMeta.timap(Payment.Id.apply)(_.uuid)
  implicit val clientNonceMeta: Meta[ClientNonce] = Meta[String].timap(new ClientNonce(_))(_.string)

  implicit val paymentStatusMeta: Meta[Payment.Status] =
    Meta[String].timap(Payment.Status.withNameInsensitive)(_.value)
}
