package io.iohk.atala.mirror.models

import java.time.Instant

import io.iohk.atala.mirror.models.CardanoAddressInfo.{CardanoAddress, RegistrationDate}
import io.iohk.atala.mirror.models.Connection.ConnectionToken

case class CardanoAddressInfo(
    cardanoAddress: CardanoAddress,
    connectionToken: ConnectionToken,
    registrationDate: RegistrationDate,
    messageId: ConnectorMessageId
)

object CardanoAddressInfo {
  case class CardanoAddress(address: String) extends AnyVal

  case class RegistrationDate(date: Instant) extends AnyVal
}
