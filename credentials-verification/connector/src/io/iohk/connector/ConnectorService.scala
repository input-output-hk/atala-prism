package io.iohk.connector

import java.util.UUID

import io.iohk.connector.errors._
import io.iohk.connector.model.payments.{ClientNonce, Payment => ConnectorPayment}
import io.iohk.connector.model.requests.CreatePaymentRequest
import io.iohk.connector.payments.BraintreePayments
import io.iohk.connector.repositories.PaymentsRepository
import io.iohk.connector.services.{ConnectionsService, MessagesService}
import io.iohk.cvp.connector.protos._
import io.iohk.cvp.crypto.ECKeys
import io.iohk.cvp.grpc.UserIdInterceptor.participantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither._
import org.slf4j.{Logger, LoggerFactory}
import io.iohk.cvp.crypto.ECKeys._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
class ConnectorService(
    connections: ConnectionsService,
    messages: MessagesService,
    braintreePayments: BraintreePayments,
    paymentsRepository: PaymentsRepository
)(
    implicit executionContext: ExecutionContext
) extends ConnectorServiceGrpc.ConnectorService
    with ErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  /** Get active connections for current participant
    *
    * Available to: Holder, Issuer, Validator
    */
  override def getConnectionsPaginated(
      request: GetConnectionsPaginatedRequest
  ): Future[GetConnectionsPaginatedResponse] = {
    implicit val loggingContext = LoggingContext("request" -> request)

    val userId = participantId()

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
      .successMap { conns =>
        GetConnectionsPaginatedResponse(conns.map(_.toProto))
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

    connections
      .getTokenInfo(new model.TokenString(request.token))
      .wrapExceptions
      .successMap { participantInfo =>
        GetConnectionTokenInfoResponse(Some(participantInfo.toProto))
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
          case _: Throwable =>
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
    val userId = participantId()

    implicit val loggingContext = LoggingContext("request" -> request, "userId" -> userId)

    connections
      .generateToken(userId)
      .wrapExceptions
      .successMap { tokenString =>
        GenerateConnectionTokenResponse(tokenString.token)
      }
  }

  /** Return messages received after given time moment, sorted in ascending order by receive time
    *
    * Available to: Issuer, Holder, Validator
    */
  override def getMessagesPaginated(request: GetMessagesPaginatedRequest): Future[GetMessagesPaginatedResponse] = {
    val userId = participantId()

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
      .successMap { msgs =>
        GetMessagesPaginatedResponse(msgs.map(_.toProto))
      }
  }

  override def getMessagesForConnection(
      request: GetMessagesForConnectionRequest
  ): Future[GetMessagesForConnectionResponse] = {
    val userId = participantId()

    implicit val loggingContext = LoggingContext("request" -> request, "userId" -> userId)

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
    val userId = participantId()

    implicit val loggingContext = LoggingContext("request" -> request, "userId" -> userId)
    val connectionId = model.ConnectionId(UUID.fromString(request.connectionId))

    messages
      .insertMessage(userId, connectionId, request.message.toByteArray)
      .wrapExceptions
      .successMap(_ => SendMessageResponse())
  }

  override def getBraintreePaymentsConfig(
      request: GetBraintreePaymentsConfigRequest
  ): Future[GetBraintreePaymentsConfigResponse] = {
    Future.successful(GetBraintreePaymentsConfigResponse(tokenizationKey = braintreePayments.tokenizationKey))
  }

  override def processPayment(request: ProcessPaymentRequest): Future[ProcessPaymentResponse] = {
    val userId = participantId()
    implicit val loggingContext = LoggingContext("request" -> request, "userId" -> userId)

    val nonce = new ClientNonce(request.nonce)
    val amount = BigDecimal(request.amount)

    def tryProcessingPayment: FutureEither[Nothing, ConnectorPayment] = {
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

    val result = for {
      maybe <- paymentsRepository.find(userId, nonce)
      payment <- maybe match {
        case None => tryProcessingPayment
        case Some(payment) => Future.successful(Right(payment)).toFutureEither
      }
    } yield payment

    result.value
      .map {
        case Left(_) => throw new RuntimeException("Impossible")
        case Right(p) =>
          ProcessPaymentResponse().withPayment(toPaymentProto(p))
      }
  }

  override def getPayments(request: GetPaymentsRequest): Future[GetPaymentsResponse] = {
    val userId = participantId()

    paymentsRepository.find(userId).value.map {
      case Left(_) => throw new RuntimeException("Impossible")
      case Right(payments) => GetPaymentsResponse(payments.map(toPaymentProto))
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
