package io.iohk.atala.mirror.models

import io.iohk.atala.mirror.models.PayIdMessage.RawPaymentInformation

case class PayIdMessage(connectorMessageId: ConnectorMessageId, rawPaymentInformation: RawPaymentInformation)

object PayIdMessage {
  case class RawPaymentInformation(raw: String) extends AnyVal
}
