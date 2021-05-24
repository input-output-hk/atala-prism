package io.iohk.atala.mirror.models

import java.util.UUID
import java.time.Instant

import io.iohk.atala.prism.models.{UUIDValue, ConnectionToken}

final case class CardanoWallet(
    id: CardanoWallet.Id,
    name: Option[String],
    connectionToken: ConnectionToken,
    extendedPublicKey: String,
    lastGeneratedNo: Int,
    lastUsedNo: Int,
    registrationDate: CardanoWallet.RegistrationDate
)

object CardanoWallet {
  case class Id(uuid: UUID) extends AnyVal with UUIDValue
  object Id extends UUIDValue.Builder[Id]
  case class RegistrationDate(date: Instant) extends AnyVal
}
