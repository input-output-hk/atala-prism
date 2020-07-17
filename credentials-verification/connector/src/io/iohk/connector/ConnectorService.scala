package io.iohk.connector

import java.util.UUID

import com.google.protobuf.ByteString
import io.grpc.Context
import io.iohk.atala.crypto.{EC, ECPublicKey}
import io.iohk.connector.errors._
import io.iohk.connector.model._
import io.iohk.connector.model.payments.{ClientNonce, Payment => ConnectorPayment}
import io.iohk.connector.model.requests.CreatePaymentRequest
import io.iohk.connector.payments.BraintreePayments
import io.iohk.connector.repositories.{ParticipantsRepository, PaymentsRepository}
import io.iohk.connector.services.{ConnectionsService, MessagesService, RegistrationService}
import io.iohk.cvp.grpc.{GrpcAuthenticationHeader, SignedRequestsHelper}
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither._
import io.iohk.cvp.{BuildInfo, ParticipantPropagatorService}
import io.iohk.prism.protos.node_api.NodeServiceGrpc
import io.iohk.prism.protos.{connector_api, connector_models, node_api}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

class ConnectorService(
    connections: ConnectionsService,
    messages: MessagesService,
    registrationService: RegistrationService,
    braintreePayments: BraintreePayments,
    paymentsRepository: PaymentsRepository,
    authenticator: AuthenticatorWithGrpcHeaderParser,
    participantPropagatorService: ParticipantPropagatorService,
    nodeService: NodeServiceGrpc.NodeService,
    participantsRepository: ParticipantsRepository,
    // TODO: remove this flag when mobile clients implement signatures
    requireSignatureOnConnectionCreation: Boolean = false
)(implicit
    executionContext: ExecutionContext
) extends connector_api.ConnectorServiceGrpc.ConnectorService
    with ErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Retrieve a connection for a given connection token.
    *
    * Available to: Holder, Issuer, Validator
    */
  override def getConnectionByToken(
      request: connector_api.GetConnectionByTokenRequest
  ): Future[connector_api.GetConnectionByTokenResponse] = {
    authenticator.public("getConnectionByToken", request) {
      implicit val loggingContext = LoggingContext("request" -> request)
      connections
        .getConnectionByToken(new TokenString(request.token))
        .wrapExceptions
        .successMap(maybeConnection => connector_api.GetConnectionByTokenResponse(maybeConnection.map(_.toProto)))
    }
  }

  /** Get active connections for current participant
    *
    * Available to: Holder, Issuer, Validator
    */
  override def getConnectionsPaginated(
      request: connector_api.GetConnectionsPaginatedRequest
  ): Future[connector_api.GetConnectionsPaginatedResponse] = {

    def getLastSeenConnections(userId: ParticipantId): FutureEither[ConnectorError, Seq[model.ConnectionInfo]] = {
      implicit val loggingContext = LoggingContext("request" -> request)

      val lastSeenConnectionId = request.lastSeenConnectionId match {
        case "" => Right(None)
        case id =>
          Try(id)
            .map(UUID.fromString)
            .map(model.ConnectionId.apply)
            .fold(
              _ => Left(InvalidArgumentError("lastSeenConnectionId", "valid id", id).logWarn),
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
        } yield connector_api.GetConnectionsPaginatedResponse(conns.map(_.toProto))
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
      request: connector_api.GetConnectionTokenInfoRequest
  ): Future[connector_api.GetConnectionTokenInfoResponse] = {
    implicit val loggingContext = LoggingContext("request" -> request)
    authenticator.public("getConnectionTokenInfo", request) {
      connections
        .getTokenInfo(new model.TokenString(request.token))
        .wrapExceptions
        .successMap { participantInfo =>
          connector_api.GetConnectionTokenInfoResponse(Some(participantInfo.toProto))
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
      request: connector_api.AddConnectionFromTokenRequest
  ): Future[connector_api.AddConnectionFromTokenResponse] = {
    implicit val loggingContext = LoggingContext("request" -> request)

    def verifyRequestSignature(publicKey: ECPublicKey): FutureEither[ConnectorError, Unit] = {
      val ctx = Context.current()
      val header = authenticator.grpcAuthenticationHeaderParser.parse(ctx)

      header match {
        case Some(GrpcAuthenticationHeader.PublicKeyBased(requestNonce, headerPublicKey, signature)) =>
          val payload = SignedRequestsHelper.merge(requestNonce, request.toByteArray).toArray

          val resultEither = for {
            _ <- Either.cond(
              headerPublicKey == publicKey,
              (),
              InvalidArgumentError("publicKey", "key matching one in GRPC header", "different key")
            )
            _ <- Either.cond(
              EC.verify(payload, publicKey, signature),
              (),
              SignatureVerificationError()
            )
          } yield ()
          Future.successful(resultEither).toFutureEither
        case _ =>
          Future.successful(Left(SignatureMissingError())).toFutureEither
      }
    }

    def f() = {
      Future
        .fromTry(Try {
          val paymentNonce = Option(request.paymentNonce).filter(_.nonEmpty).map(s => new ClientNonce(s))
          val publicKey: ECPublicKey = request.holderEncodedPublicKey
            .map { encodedKey =>
              EC.toPublicKey(encodedKey.publicKey.toByteArray)
            }
            .getOrElse {
              // The iOS app has a key hardcoded which is not a valid ECKey
              // This hack allow us to do the demo because it generates a valid key
              // ignoring whatever cames from the app.
              // TODO: Remove me after the demo
              try {
                request.holderPublicKey
                  .map { protoKey =>
                    EC.toPublicKey(
                      x = BigInt(protoKey.x),
                      y = BigInt(protoKey.y)
                    )
                  }
                  .getOrElse(throw new RuntimeException("Missing public key"))
              } catch {
                case NonFatal(_) =>
                  EC.generateKeyPair().publicKey
              }
            }

          val result = for {
            _ <-
              if (requireSignatureOnConnectionCreation) {
                verifyRequestSignature(publicKey)
              } else Future.successful(Right(())).toFutureEither
            connectionCreationResult <-
              connections
                .addConnectionFromToken(new model.TokenString(request.token), publicKey, paymentNonce)
          } yield connectionCreationResult

          result.wrapExceptions
            .successMap {
              case (userId, connectionInfo) =>
                connector_api
                  .AddConnectionFromTokenResponse(Some(connectionInfo.toProto))
                  .withUserId(userId.uuid.toString)
            }
        })
        .flatten
    }

    authenticator.public("addConnectionFromToken", request) {
      f()
    }
  }

  /** Delete active connection
    *
    * Available to: Holder, Issuer, Validator
    *
    * Errors:
    * Connection does not exist (UNKNOWN)
    */
  override def deleteConnection(
      request: connector_api.DeleteConnectionRequest
  ): Future[connector_api.DeleteConnectionResponse] = {
    authenticator.authenticated("deleteConnection", request) { _ =>
      Future.failed(new NotImplementedError)
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
  override def registerDID(request: connector_api.RegisterDIDRequest): Future[connector_api.RegisterDIDResponse] = {
    authenticator.public("registerDID", request) {
      val createDIDOperationF = Future {
        request.createDIDOperation
          .getOrElse(throw new RuntimeException("The createDIDOperation is mandatory"))
      }
      for {
        createDIDOperation <- createDIDOperationF
        tpe = request.role match {
          case connector_api.RegisterDIDRequest.Role.issuer => ParticipantType.Issuer
          case connector_api.RegisterDIDRequest.Role.verifier => ParticipantType.Verifier
          case connector_api.RegisterDIDRequest.Role.Unrecognized(_) => throw new RuntimeException("Unknown role")
        }
        logo = ParticipantLogo(request.logo.toByteArray.toVector)
        result <-
          registrationService
            .register(tpe = tpe, logo = logo, name = request.name, createDIDOperation = createDIDOperation)
            .value
            .map {
              case Left(_) => throw new RuntimeException("Impossible")
              case Right(x) => x
            }

        _ <- participantPropagatorService.propagate(id = result.id, tpe = tpe)
      } yield connector_api.RegisterDIDResponse(did = result.did)
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
  override def changeBillingPlan(
      request: connector_api.ChangeBillingPlanRequest
  ): Future[connector_api.ChangeBillingPlanResponse] = {
    authenticator.authenticated("changeBillingPlan", request) { _ =>
      Future.failed(new NotImplementedError)
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
      request: connector_api.GenerateConnectionTokenRequest
  ): Future[connector_api.GenerateConnectionTokenResponse] = {
    def f(userId: ParticipantId) = {

      implicit val loggingContext: LoggingContext = LoggingContext("request" -> request, "userId" -> userId)
      connections
        .generateToken(userId)
        .wrapExceptions
        .successMap { tokenString =>
          connector_api.GenerateConnectionTokenResponse(tokenString.token)
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
  override def getMessagesPaginated(
      request: connector_api.GetMessagesPaginatedRequest
  ): Future[connector_api.GetMessagesPaginatedResponse] = {

    def getLastSeenMessages(userId: ParticipantId): FutureEither[ConnectorError, Seq[Message]] = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> userId)

      val lastSeenMessageId = request.lastSeenMessageId match {
        case "" => Right(None)
        case id =>
          Try(id)
            .map(UUID.fromString)
            .map(model.MessageId.apply)
            .fold(
              _ => Left(InvalidArgumentError("lastSeenMessageId", "valid id", id).logWarn),
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
        } yield connector_api.GetMessagesPaginatedResponse(msgs.map(_.toProto))
      }.successMap(identity)
    }

    authenticator.authenticated("getMessagesPaginated", request) { participantId =>
      f(participantId)
    }
  }

  override def getMessagesForConnection(
      request: connector_api.GetMessagesForConnectionRequest
  ): Future[connector_api.GetMessagesForConnectionResponse] = {

    def f(userId: ParticipantId) = {
      Future {
        implicit val loggingContext: LoggingContext = LoggingContext("request" -> request, "userId" -> userId)
        val validatedConnectionId = Try(request.connectionId)
          .map(UUID.fromString)
          .map(model.ConnectionId.apply)
          .fold(
            _ => Left(InvalidArgumentError("connectionId", "valid id", request.connectionId).logWarn),
            id => Right(id)
          )

        validatedConnectionId.toFutureEither
          .flatMap(connectionId => messages.getMessages(userId, connectionId))
          .wrapExceptions
          .successMap { msgs =>
            connector_api.GetMessagesForConnectionResponse(msgs.map(_.toProto))
          }
      }.flatten
    }

    authenticator.authenticated("getMessagesForConnection", request) { participantId =>
      f(participantId)
    }
  }

  /** Returns public keys that can be used for secure communication with the other end of connection
    */
  override def getConnectionCommunicationKeys(
      request: connector_api.GetConnectionCommunicationKeysRequest
  ): Future[connector_api.GetConnectionCommunicationKeysResponse] = {

    def f(userId: ParticipantId) = {
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> userId)

      Future
        .fromTry(Try(Right(ConnectionId.apply(request.connectionId))))
        .toFutureEither
        .flatMap { connectionId =>
          connections.getConnectionCommunicationKeys(connectionId, userId)
        }
        .wrapExceptions
        .successMap { keys =>
          connector_api.GetConnectionCommunicationKeysResponse(
            keys = keys.map {
              case (keyId, key) =>
                connector_models.ConnectionKey(
                  keyId = keyId,
                  key = Some(
                    connector_models.EncodedPublicKey(
                      publicKey = ByteString.copyFrom(key.getEncoded)
                    )
                  )
                )
            }
          )
        }
    }

    authenticator.authenticated("getConnectionCommunicationKeys", request) { participantId =>
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
  override def sendMessage(request: connector_api.SendMessageRequest): Future[connector_api.SendMessageResponse] = {

    def f(userId: ParticipantId) = {
      Future {
        implicit val loggingContext = LoggingContext("request" -> request, "userId" -> userId)

        messages
          .insertMessage(userId, ConnectionId(request.connectionId), request.message.toByteArray)
          .wrapExceptions
          .successMap(_ => connector_api.SendMessageResponse())
      }.flatten
    }

    authenticator.authenticated("sendMessage", request) { participantId =>
      f(participantId)
    }
  }

  override def getBraintreePaymentsConfig(
      request: connector_api.GetBraintreePaymentsConfigRequest
  ): Future[connector_api.GetBraintreePaymentsConfigResponse] = {
    authenticator.public("getBraintreePaymentsConfig", request) {
      Future.successful(
        connector_api.GetBraintreePaymentsConfigResponse(tokenizationKey = braintreePayments.tokenizationKey)
      )
    }
  }

  override def processPayment(
      request: connector_api.ProcessPaymentRequest
  ): Future[connector_api.ProcessPaymentResponse] = {

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
              connector_api.ProcessPaymentResponse().withPayment(toPaymentProto(p))
          }
      }.flatten

    }

    authenticator.authenticated("processPayment", request) { participantId =>
      f(participantId)
    }
  }

  override def getPayments(request: connector_api.GetPaymentsRequest): Future[connector_api.GetPaymentsResponse] = {
    def f(userId: ParticipantId) = {
      paymentsRepository.find(userId).value.map {
        case Left(_) => throw new RuntimeException("Impossible")
        case Right(payments) => connector_api.GetPaymentsResponse(payments.map(toPaymentProto))
      }
    }

    authenticator.authenticated("getPayments", request) { participantId =>
      f(participantId)
    }
  }

  private def toPaymentProto(payment: ConnectorPayment): connector_models.Payment = {
    connector_models
      .Payment()
      .withAmount(payment.amount.toString())
      .withCreatedOn(payment.createdOn.toEpochMilli)
      .withId(payment.id.uuid.toString)
      .withStatus(payment.status.entryName)
      .withFailureReason(payment.failureReason.getOrElse(""))
  }

  override def getBuildInfo(request: connector_api.GetBuildInfoRequest): Future[connector_api.GetBuildInfoResponse] = {
    nodeService
      .getBuildInfo(node_api.GetBuildInfoRequest())
      .map(nodeBuildInfo =>
        connector_api
          .GetBuildInfoResponse()
          .withVersion(BuildInfo.version)
          .withScalaVersion(BuildInfo.scalaVersion)
          .withMillVersion(BuildInfo.millVersion)
          .withBuildTime(BuildInfo.buildTime)
          .withNodeVersion(nodeBuildInfo.version)
      )
  }

  override def getCurrentUser(
      request: connector_api.GetCurrentUserRequest
  ): Future[connector_api.GetCurrentUserResponse] = {
    authenticator.authenticated("getCurrentUser", request) { participantId =>
      participantsRepository
        .findBy(participantId)
        .map { info =>
          val role = info.tpe match {
            case ParticipantType.Holder =>
              throw new NotImplementedError("This method is not available for holders right now")

            case ParticipantType.Issuer => connector_api.GetCurrentUserResponse.Role.issuer
            case ParticipantType.Verifier => connector_api.GetCurrentUserResponse.Role.verifier
          }
          val logo = info.logo.map(_.bytes).getOrElse(Vector.empty).toArray
          connector_api
            .GetCurrentUserResponse()
            .withName(info.name)
            .withRole(role)
            .withLogo(ByteString.copyFrom(logo))
        }
        .successMap(identity)
    }
  }
}
