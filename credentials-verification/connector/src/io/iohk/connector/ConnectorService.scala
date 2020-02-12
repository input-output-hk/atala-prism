package io.iohk.connector

import java.util.UUID

import io.iohk.connector.errors._
import io.iohk.connector.model.Message
import io.iohk.connector.model.payments.{ClientNonce, Payment => ConnectorPayment}
import io.iohk.connector.model.requests.CreatePaymentRequest
import io.iohk.connector.payments.BraintreePayments
import io.iohk.connector.repositories.PaymentsRepository
import io.iohk.connector.services.{ConnectionsService, MessagesService}
import io.iohk.cvp.connector.protos._
import io.iohk.cvp.crypto.ECKeys
import io.iohk.cvp.crypto.ECKeys._
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal
class ConnectorService(
    connections: ConnectionsService,
    messages: MessagesService,
    braintreePayments: BraintreePayments,
    paymentsRepository: PaymentsRepository,
    authenticator: Authenticator
)(
    implicit executionContext: ExecutionContext
) extends ConnectorServiceGrpc.ConnectorService
    with ErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Get active connections for current participant
    *
    * Available to: Holder, Issuer, Validator
    */
  override def getConnectionsPaginated(
      request: GetConnectionsPaginatedRequest
  ): Future[GetConnectionsPaginatedResponse] = {

    def getLastSeenConnections(userId: ParticipantId): FutureEither[ConnectorError, Seq[model.ConnectionInfo]] = {
      implicit val loggingContext = LoggingContext("request" -> request)

      val lastSeenConnectionId = request.lastSeenConnectionId match {
        case "" => Right(None)
        case id =>
          Try(id)
            .map(UUID.fromString)
            .map(model.ConnectionId.apply)
            .fold(
              ex => Left(InvalidArgumentError("lastSeenConnectionId", "valid id", id).logWarn),
              id => Right(Some(id))
            )
      }
      lastSeenConnectionId.toFutureEither
        .flatMap(idOpt => connections.getConnectionsPaginated(userId, request.limit, idOpt))
        .wrapExceptions
    }

    def f(participantId: ParticipantId) = {
      {
        for {
          conns <- getLastSeenConnections(participantId)
        } yield GetConnectionsPaginatedResponse(conns.map(_.toProto))
      }.successMap(identity)
    }

    authenticator.authenticated("getConnectionsPaginated", request) { participantId =>
      f(participantId)
    }

  }

  /** Return info about connection token such as creator info
    *
    * Available to: Holder
    *
    * Errors:
    * Token does not exist (UNKNOWN)
    */
  override def getConnectionTokenInfo(
      request: GetConnectionTokenInfoRequest
  ): Future[GetConnectionTokenInfoResponse] = {
    implicit val loggingContext = LoggingContext("request" -> request)
    authenticator.public("getConnectionTokenInfo", request) {
      connections
        .getTokenInfo(new model.TokenString(request.token))
        .wrapExceptions
        .successMap { participantInfo =>
          GetConnectionTokenInfoResponse(Some(participantInfo.toProto))
        }
    }
  }

  /** Instantiate connection from connection token
    *
    * Available to: Holder
    *
    * Errors:
    * Token does not exist (UNKNOWN)
    */
  override def addConnectionFromToken(
      request: AddConnectionFromTokenRequest
  ): Future[AddConnectionFromTokenResponse] = {
    implicit val loggingContext = LoggingContext("request" -> request)
    def f() = {
      Future {
        val paymentNonce = Option(request.paymentNonce).filter(_.nonEmpty).map(s => new ClientNonce(s))
        val publicKey = request.holderEncodedPublicKey
          .map { encodedKey =>
            io.iohk.cvp.crypto.ECKeys.EncodedPublicKey(encodedKey.publicKey.toByteArray.toVector)
          }
          .getOrElse {
            // The iOS app has a key hardcoded which is not a valid ECKey
            // This hack allow us to do the demo because it generates a valid key
            // ignoring whatever cames from the app.
            // TODO: Remove me after the demo
            try {
              request.holderPublicKey
                .map { protoKey =>
                  toEncodePublicKey(
                    toPublicKey(
                      x = BigInt(protoKey.x),
                      y = BigInt(protoKey.y)
                    )
                  )
                }
                .getOrElse(throw new RuntimeException("Missing public key"))
            } catch {
              case NonFatal(e) =>
                toEncodePublicKey(ECKeys.generateKeyPair().getPublic)
            }
          }

        connections
          .addConnectionFromToken(new model.TokenString(request.token), publicKey, paymentNonce)
          .wrapExceptions
          .successMap {
            case (userId, connectionInfo) =>
              AddConnectionFromTokenResponse(Some(connectionInfo.toProto)).withUserId(userId.uuid.toString)
          }
      }.flatten
    }

    authenticator.public("addConnectionFromToken", request) { f() }
  }

  /** Delete active connection
    *
    * Available to: Holder, Issuer, Validator
    *
    * Errors:
    * Connection does not exist (UNKNOWN)
    */
  override def deleteConnection(request: DeleteConnectionRequest): Future[DeleteConnectionResponse] = {
    Future.successful {
      DeleteConnectionResponse()
    }
  }

  /** Bind DID to issuer
    *
    * Available to: Issuer
    *
    * Errors:
    * Invalid DID (INVALID_ARGUMENT)
    * Invalid DID document (INVALID_ARGUMENT)
    * DID Document does not match DID (INVALID_ARGUMENT)
    */
  override def registerDID(request: RegisterDIDRequest): Future[RegisterDIDResponse] = {
    Future.successful {
      RegisterDIDResponse()
    }
  }

  /** Change billing plan of participant who wants to generate connection tokens
    *
    * Available to: Issuer, Validator
    *
    * Errors:
    * Unknown billing plan (UNKNOWN)
    * User not allowed to set this billing plan (PERMISSION_DENIED)
    */
  override def changeBillingPlan(request: ChangeBillingPlanRequest): Future[ChangeBillingPlanResponse] = {
    Future.successful {
      ChangeBillingPlanResponse()
    }
  }

  /** Generate connection token that can be used to instantiate connection
    *
    * Available to: Issuer, Validator
    *
    * Errors:
    * Billing plan doesn't allow token generation (PERMISSION_DENIED)
    */
  override def generateConnectionToken(
      request: GenerateConnectionTokenRequest
  ): Future[GenerateConnectionTokenResponse] = {
    def f(userId: ParticipantId) = {

      implicit val loggingContext: LoggingContext = LoggingContext("request" -> request, "userId" -> userId)
      connections
        .generateToken(userId)
        .wrapExceptions
        .successMap { tokenString =>
          GenerateConnectionTokenResponse(tokenString.token)
        }

    }
    authenticator.authenticated("generateConnectionToken", request) { participantId =>
      f(participantId)
    }

  }

  /** Return messages received after given time moment, sorted in ascending order by receive time
    *
    * Available to: Issuer, Holder, Validator
    */
  override def getMessagesPaginated(request: GetMessagesPaginatedRequest): Future[GetMessagesPaginatedResponse] = {

    def getLastSeenMessages(userId: ParticipantId): FutureEither[ConnectorError, Seq[Message]] = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> userId)

      val lastSeenMessageId = request.lastSeenMessageId match {
        case "" => Right(None)
        case id =>
          Try(id)
            .map(UUID.fromString)
            .map(model.MessageId.apply)
            .fold(
              ex => Left(InvalidArgumentError("lastSeenMessageId", "valid id", id).logWarn),
              id => Right(Some(id))
            )
      }
      lastSeenMessageId.toFutureEither
        .flatMap(idOpt => messages.getMessagesPaginated(userId, request.limit, idOpt))
        .wrapExceptions
    }

    def f(participantId: ParticipantId) = {
      {
        for {
          msgs <- getLastSeenMessages(participantId)
        } yield GetMessagesPaginatedResponse(msgs.map(_.toProto))
      }.successMap(identity)
    }

    authenticator.authenticated("getMessagesPaginated", request) { participantId =>
      f(participantId)
    }

  }

  override def getMessagesForConnection(
      request: GetMessagesForConnectionRequest
  ): Future[GetMessagesForConnectionResponse] = {

    def f(userId: ParticipantId) = {
      Future {
        implicit val loggingContext: LoggingContext = LoggingContext("request" -> request, "userId" -> userId)
        val validatedConnectionId = Try(request.connectionId)
          .map(UUID.fromString)
          .map(model.ConnectionId.apply)
          .fold(
            ex => Left(InvalidArgumentError("connectionId", "valid id", request.connectionId).logWarn),
            id => Right(id)
          )

        validatedConnectionId.toFutureEither
          .flatMap(connectionId => messages.getMessages(userId, connectionId))
          .wrapExceptions
          .successMap { msgs =>
            GetMessagesForConnectionResponse(msgs.map(_.toProto))
          }
      }.flatten
    }

    authenticator.authenticated("getMessagesForConnection", request) { participantId =>
      f(participantId)
    }

  }

  /** Send message over a connection
    *
    * Available to: Issuer, Holder, Validator
    *
    * Errors:
    * Unknown connection (UNKNOWN)
    * Connection closed (FAILED_PRECONDITION)
    */
  override def sendMessage(request: SendMessageRequest): Future[SendMessageResponse] = {

    def f(userId: ParticipantId) = {
      Future {
        implicit val loggingContext = LoggingContext("request" -> request, "userId" -> userId)
        val connectionId = model.ConnectionId(UUID.fromString(request.connectionId))

        messages
          .insertMessage(userId, connectionId, request.message.toByteArray)
          .wrapExceptions
          .successMap(_ => SendMessageResponse())
      }.flatten
    }

    authenticator.authenticated("sendMessage", request) { participantId =>
      f(participantId)
    }
  }

  override def getBraintreePaymentsConfig(
      request: GetBraintreePaymentsConfigRequest
  ): Future[GetBraintreePaymentsConfigResponse] = {
    authenticator.public("getBraintreePaymentsConfig", request) {
      Future.successful(GetBraintreePaymentsConfigResponse(tokenizationKey = braintreePayments.tokenizationKey))
    }
  }

  override def processPayment(request: ProcessPaymentRequest): Future[ProcessPaymentResponse] = {

    def tryProcessingPayment(
        userId: ParticipantId,
        amount: BigDecimal,
        nonce: ClientNonce
    ): FutureEither[Nothing, ConnectorPayment] = {
      braintreePayments
        .processPayment(amount, nonce)
        .value
        .map {
          case Left(error) => CreatePaymentRequest(nonce, amount, ConnectorPayment.Status.Failed, Some(error.reason))
          case Right(_) => CreatePaymentRequest(nonce, amount, ConnectorPayment.Status.Charged, None)
        }
        .flatMap { r =>
          paymentsRepository.create(userId, r).value
        }
        .toFutureEither
    }

    def f(userId: ParticipantId) = {
      Future {
        val nonce: ClientNonce = new ClientNonce(request.nonce)
        val amount: BigDecimal = BigDecimal(request.amount)

        implicit val loggingContext = LoggingContext("request" -> request, "userId" -> userId)

        val result = for {
          maybe <- paymentsRepository.find(userId, nonce)
          payment <- maybe match {
            case None => tryProcessingPayment(userId, amount, nonce)
            case Some(payment) => Future.successful(Right(payment)).toFutureEither
          }
        } yield payment

        result.value
          .map {
            case Left(_) => throw new RuntimeException("Impossible")
            case Right(p) =>
              ProcessPaymentResponse().withPayment(toPaymentProto(p))
          }
      }.flatten

    }

    authenticator.authenticated("processPayment", request) { participantId =>
      f(participantId)
    }

  }

  override def getPayments(request: GetPaymentsRequest): Future[GetPaymentsResponse] = {
    def f(userId: ParticipantId) = {
      paymentsRepository.find(userId).value.map {
        case Left(_) => throw new RuntimeException("Impossible")
        case Right(payments) => GetPaymentsResponse(payments.map(toPaymentProto))
      }
    }

    authenticator.authenticated("getPayments", request) { participantId =>
      f(participantId)
    }
  }

  private def toPaymentProto(payment: ConnectorPayment): Payment = {
    Payment()
      .withAmount(payment.amount.toString())
      .withCreatedOn(payment.createdOn.toEpochMilli)
      .withId(payment.id.uuid.toString)
      .withStatus(payment.status.entryName)
      .withFailureReason(payment.failureReason.getOrElse(""))
  }
}
