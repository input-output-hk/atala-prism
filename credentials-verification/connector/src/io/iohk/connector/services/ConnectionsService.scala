package io.iohk.connector.services

import io.iohk.connector.errors._
import io.iohk.connector.model._
import io.iohk.connector.model.payments.{ClientNonce, Payment}
import io.iohk.connector.model.requests.CreatePaymentRequest
import io.iohk.connector.payments.BraintreePayments
import io.iohk.connector.repositories.{ConnectionsRepository, PaymentsRepository}
import io.iohk.cvp.crypto.ECKeys.EncodedPublicKey
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.{ExecutionContext, Future}

class ConnectionsService(
    connectionsRepository: ConnectionsRepository,
    paymentsRepository: PaymentsRepository,
    braintreePayments: BraintreePayments
)(implicit ec: ExecutionContext) {
  def generateToken(userId: ParticipantId): FutureEither[ConnectorError, TokenString] = {
    connectionsRepository.insertToken(userId, TokenString.random())
  }

  def getTokenInfo(token: TokenString): FutureEither[ConnectorError, ParticipantInfo] = {
    connectionsRepository.getTokenInfo(token)
  }

  def addConnectionFromToken(
      tokenString: TokenString,
      publicKey: EncodedPublicKey,
      paymentNonce: Option[ClientNonce]
  ): FutureEither[ConnectorError, (ParticipantId, ConnectionInfo)] = {
    val connectionPrice = 5
    def tryProcessingPayment() = paymentNonce match {
      case Some(value) => braintreePayments.processPayment(connectionPrice, value).map(Option.apply)
      case None => Future.successful(Right(Option.empty)).toFutureEither
    }

    def tryStoringPayment(userId: ParticipantId) = paymentNonce match {
      case Some(value) =>
        val request = CreatePaymentRequest(value, connectionPrice, Payment.Status.Charged, None)
        paymentsRepository.create(userId, request).map(Option.apply)

      case None =>
        Future.successful(Right(Option.empty)).toFutureEither
    }

    for {
      _ <- tryProcessingPayment().failOnLeft(e => new RuntimeException(s"Failed to process payment: ${e.reason}"))
      x <- connectionsRepository.addConnectionFromToken(tokenString, publicKey)
      _ <- tryStoringPayment(x._1)
    } yield x
  }

  def getConnectionsPaginated(
      userId: ParticipantId,
      limit: Int,
      lastSeenConnectionId: Option[ConnectionId]
  ): FutureEither[ConnectorError, Seq[ConnectionInfo]] = {
    connectionsRepository.getConnectionsPaginated(userId, limit, lastSeenConnectionId)
  }
}
