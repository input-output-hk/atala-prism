package io.iohk.connector.model.requests

import io.iohk.connector.model.payments.{ClientNonce, Payment}

case class CreatePaymentRequest(
    nonce: ClientNonce,
    amount: BigDecimal,
    status: Payment.Status,
    failureReason: Option[String]
)
