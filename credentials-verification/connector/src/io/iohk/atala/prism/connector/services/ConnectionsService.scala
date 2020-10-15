package io.iohk.atala.prism.connector.services

import io.iohk.atala.prism.crypto.{EC, ECConfig, ECPublicKey}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.connector.errors._
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.model.payments.{ClientNonce, Payment}
import io.iohk.atala.prism.connector.model.requests.CreatePaymentRequest
import io.iohk.atala.prism.connector.payments.BraintreePayments
import io.iohk.atala.prism.connector.repositories.{ConnectionsRepository, PaymentsRepository}
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class ConnectionsService(
    connectionsRepository: ConnectionsRepository,
    paymentsRepository: PaymentsRepository,
    braintreePayments: BraintreePayments,
    nodeService: NodeServiceGrpc.NodeService
)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getConnectionByToken(token: TokenString): FutureEither[ConnectorError, Option[Connection]] = {
    connectionsRepository.getConnectionByToken(token)
  }

  def generateToken(userId: ParticipantId): FutureEither[ConnectorError, TokenString] = {
    connectionsRepository.insertToken(userId, TokenString.random())
  }

  def getTokenInfo(token: TokenString): FutureEither[ConnectorError, ParticipantInfo] = {
    connectionsRepository.getTokenInfo(token)
  }

  def addConnectionFromToken(
      tokenString: TokenString,
      publicKey: ECPublicKey,
      paymentNonce: Option[ClientNonce]
  ): FutureEither[ConnectorError, (ParticipantId, ConnectionInfo)] = {
    val connectionPrice = 5
    def tryProcessingPayment() =
      paymentNonce match {
        case Some(value) => braintreePayments.processPayment(connectionPrice, value).map(Option.apply)
        case None => Future.successful(Right(Option.empty)).toFutureEither
      }

    def tryStoringPayment(userId: ParticipantId) =
      paymentNonce match {
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

  def getConnectionCommunicationKeys(
      connectionId: ConnectionId,
      userId: ParticipantId
  ): FutureEither[ConnectorError, Seq[(String, ECPublicKey)]] = {
    def getDidCommunicationKeys(did: String): FutureEither[ConnectorError, Seq[(String, ECPublicKey)]] = {
      val request = node_api.GetDidDocumentRequest(did = did)
      val result = for {
        response <- nodeService.getDidDocument(request)
        allKeys = response.document.map(_.publicKeys).getOrElse(Seq.empty)
        validKeys = allKeys.filter(key => key.revokedOn.isEmpty)
        // TODO: select communication keys only, once we provision them and make frontend use them
      } yield validKeys.map { key =>
        val keyData = key.keyData.ecKeyData.getOrElse(throw new Exception("Node returned key without keyData"))
        assert(keyData.curve == ECConfig.CURVE_NAME)
        (key.id, EC.toPublicKey(keyData.x.toByteArray, keyData.y.toByteArray))
      }

      result.map(Right(_)).recover { case ex => Left(InternalServerError(ex)) }.toFutureEither
    }

    for {
      participantInfo <- connectionsRepository.getOtherSideInfo(connectionId, userId).map(_.get)
      keys <- (participantInfo.did, participantInfo.publicKey) match {
        case (Some(did), keyOpt) =>
          if (keyOpt.isDefined) {
            logger.warn(s"Both DID and keys found for user ${userId}, using DID keys only")
          }
          getDidCommunicationKeys(did)
        case (None, Some(key)) => Future.successful(Right(Seq(("", key)))).toFutureEither
        case (None, None) => Future.successful(Right(Seq.empty)).toFutureEither
      }
    } yield keys
  }
}
