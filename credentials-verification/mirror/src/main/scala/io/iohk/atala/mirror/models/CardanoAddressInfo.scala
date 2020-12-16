package io.iohk.atala.mirror.models

import java.time.Instant

import io.iohk.atala.mirror.models.CardanoAddressInfo.{CardanoAddress, CardanoNetwork, RegistrationDate}
import io.iohk.atala.prism.models.{ConnectionToken, ConnectorMessageId}
import io.iohk.atala.prism.mirror.payid.Address.VerifiedAddress

case class CardanoAddressInfo(
    cardanoAddress: CardanoAddress,
    payidVerifiedAddress: Option[VerifiedAddress],
    cardanoNetwork: CardanoNetwork,
    connectionToken: ConnectionToken,
    registrationDate: RegistrationDate,
    messageId: ConnectorMessageId
)

object CardanoAddressInfo {
  case class CardanoAddress(address: String) extends AnyVal

  case class RegistrationDate(date: Instant) extends AnyVal

  case class CardanoNetwork(network: String) extends AnyVal
}
