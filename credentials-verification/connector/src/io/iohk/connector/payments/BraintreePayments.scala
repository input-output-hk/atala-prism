package io.iohk.connector.payments

import com.braintreegateway.{BraintreeGateway, Environment, Transaction, TransactionRequest}
import io.iohk.connector.payments.BraintreePayments._
import io.iohk.cvp.models.ParticipantId

class BraintreePayments(gateway: BraintreeGateway, config: Config) {

  def tokenizationKey: String = config.tokenizationKey

  def processPayment(userId: ParticipantId, amount: BigDecimal, nonce: ClientNonce): Transaction = {
    // TODO: Find a way to link our userId on the braintree payments
    // their customer id seems to be case insensive while our user id is not.
    val request = new TransactionRequest()
      .amount(amount.bigDecimal)
      .paymentMethodNonce(nonce.string)
      .options()
      .submitForSettlement(true)
      .done()

    val result = gateway.transaction().sale(request)
    if (result.isSuccess) {
      result.getTarget
    } else {
      throw new RuntimeException(s"Failed to process payment: ${result.getMessage}")
    }
  }
}

object BraintreePayments {
  case class Config(
      production: Boolean,
      publicKey: String,
      privateKey: String,
      merchantId: String,
      tokenizationKey: String
  )

  class ClientNonce(val string: String) extends AnyVal

  def apply(config: Config): BraintreePayments = {
    val environment = if (config.production) Environment.PRODUCTION else Environment.SANDBOX
    val gateway = new BraintreeGateway(
      environment,
      config.merchantId,
      config.publicKey,
      config.privateKey
    )

    new BraintreePayments(gateway, config)
  }
}
