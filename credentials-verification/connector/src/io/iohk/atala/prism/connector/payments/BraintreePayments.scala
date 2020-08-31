package io.iohk.atala.prism.connector.payments

import com.braintreegateway.{BraintreeGateway, Environment, Transaction, TransactionRequest}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.connector.model.payments.ClientNonce
import io.iohk.atala.prism.connector.payments.BraintreePayments._

import scala.concurrent.{ExecutionContext, Future}

class BraintreePayments(gateway: BraintreeGateway, config: Config)(implicit ec: ExecutionContext) {

  def tokenizationKey: String = config.tokenizationKey

  def processPayment(
      amount: BigDecimal,
      nonce: ClientNonce
  ): FutureEither[PaymentError, Transaction] = {
    val f = Future {
      // TODO: Find a way to link our userId on the braintree payments
      // their customer id seems to be case insensitive while our user id is not.
      val request = new TransactionRequest()
        .amount(amount.bigDecimal)
        .paymentMethodNonce(nonce.string)
        .options()
        .submitForSettlement(true)
        .done()

      val result = gateway.transaction().sale(request)
      if (result.isSuccess) {
        Right(result.getTarget)
      } else {
        Left(PaymentError(result.getMessage))
      }
    }
    f.toFutureEither
  }
}

object BraintreePayments {
  case class PaymentError(reason: String)

  case class Config(
      production: Boolean,
      publicKey: String,
      privateKey: String,
      merchantId: String,
      tokenizationKey: String
  )

  def apply(config: Config)(implicit ec: ExecutionContext): BraintreePayments = {
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
