package io.iohk.atala.prism.connector.model.requests

import io.iohk.atala.prism.connector.model.payments.{ClientNonce, Payment}

case class CreatePaymentRequest(
    nonce: ClientNonce,
    amount: BigDecimal,
    status: Payment.Status,
    failureReason: Option[String]
)
