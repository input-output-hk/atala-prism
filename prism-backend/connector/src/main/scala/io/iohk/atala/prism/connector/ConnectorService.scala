package io.iohk.atala.prism.connector

import cats.effect._
import com.google.protobuf.ByteString
import io.grpc.Context
import io.grpc.stub.StreamObserver
import io.iohk.atala.prism.BuildInfo
import io.iohk.atala.prism.auth.AuthenticatorWithGrpcHeaderParser
import io.iohk.atala.prism.auth.grpc.{GrpcAuthenticationHeader, SignedRequestsHelper}
import io.iohk.atala.prism.connector.errors._
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository
import io.iohk.atala.prism.connector.services.{
  ConnectionsService,
  MessageNotificationService,
  MessagesService,
  RegistrationService
}
import io.iohk.atala.prism.crypto.{EC, ECPublicKey}
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.models.{ParticipantId, ProtoCodecs}
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.connector_api.{GetMessageStreamRequest, GetMessageStreamResponse}
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.protos.{connector_api, connector_models, node_api}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ConnectorService(
    connections: ConnectionsService,
    messages: MessagesService,
    registrationService: RegistrationService,
    messageNotificationService: MessageNotificationService,
    authenticator: AuthenticatorWithGrpcHeaderParser[ParticipantId],
    nodeService: NodeServiceGrpc.NodeService,
    participantsRepository: ParticipantsRepository
)(implicit
    executionContext: ExecutionContext
) extends connector_api.ConnectorServiceGrpc.ConnectorService
    with ConnectorErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private implicit val contextSwitch: ContextShift[IO] = IO.contextShift(executionContext)

  override def healthCheck(request: HealthCheckRequest): Future[HealthCheckResponse] =
    Future.successful(HealthCheckResponse())

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
          model.ConnectionId
            .from(id)
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
          connector_api.GetConnectionTokenInfoResponse(
            creator = Some(participantInfo.toProto),
            creatorName = participantInfo.name,
            creatorLogo = ByteString.copyFrom(participantInfo.logo.map(_.bytes).getOrElse(Vector.empty).toArray),
            creatorDID = participantInfo.did.map(_.value).getOrElse("")
          )
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
          val publicKey: ECPublicKey = request.holderEncodedPublicKey
            .map { encodedKey =>
              EC.toPublicKey(encodedKey.publicKey.toByteArray)
            }
            .getOrElse(throw new RuntimeException("The encoded public key is required to accept a connection"))

          val result = for {
            _ <- verifyRequestSignature(publicKey)
            connectionCreationResult <-
              connections.addConnectionFromToken(new model.TokenString(request.token), publicKey)
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
      } yield connector_api
        .RegisterDIDResponse(did = result.did.value)
        .withTransactionInfo(ProtoCodecs.toTransactionInfo(result.transactionInfo))
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

    def getLastSeenMessages(participantId: ParticipantId): FutureEither[ConnectorError, Seq[Message]] = {
      implicit val loggingContext = LoggingContext("request" -> request, "participantId" -> participantId)

      getMessageIdField(request.lastSeenMessageId, "lastSeenMessageId").toFutureEither
        .flatMap(idOpt => messages.getMessagesPaginated(participantId, request.limit, idOpt))
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

  override def getMessageStream(
      request: GetMessageStreamRequest,
      responseObserver: StreamObserver[GetMessageStreamResponse]
  ): Unit = {
    def streamMessages(recipientId: ParticipantId, lastSeenMessageId: Option[MessageId]): Unit = {
      val existingMessageStream =
        messages.getMessageStream(recipientId = recipientId, lastSeenMessageId = lastSeenMessageId)
      val newMessageStream = messageNotificationService.stream(recipientId)
      (existingMessageStream ++ newMessageStream)
        .map(message => responseObserver.onNext(connector_api.GetMessageStreamResponse().withMessage(message.toProto)))
        .compile
        .drain
        .unsafeToFuture()
        .onComplete {
          case Success(_) => responseObserver.onCompleted()
          case Failure(exception) =>
            logger.warn(s"Could not stream messages for recipient $recipientId", exception)
            responseObserver.onError(exception)
        }
    }

    def f(participantId: ParticipantId): Future[Unit] = {
      implicit val loggingContext = LoggingContext("request" -> request, "participantId" -> participantId)

      for {
        lastSeenMessageId <- getMessageIdField(request.lastSeenMessageId, "lastSeenMessageId").toFutureEither.flatten
        _ = streamMessages(participantId, lastSeenMessageId)
      } yield ()
    }

    authenticator.authenticated("getMessageStream", request) { participantId =>
      f(participantId)
    }
    ()
  }

  private def getMessageIdField(
      id: String,
      fieldName: String
  )(implicit lc: LoggingContext): Either[ConnectorError, Option[model.MessageId]] = {
    id match {
      case "" => Right(None)
      case id =>
        model.MessageId
          .from(id)
          .fold(
            _ => Left(InvalidArgumentError(fieldName, "valid id", id).logWarn),
            id => Right(Some(id))
          )
    }
  }

  override def getMessagesForConnection(
      request: connector_api.GetMessagesForConnectionRequest
  ): Future[connector_api.GetMessagesForConnectionResponse] = {

    def f(userId: ParticipantId) = {
      Future {
        implicit val loggingContext: LoggingContext = LoggingContext("request" -> request, "userId" -> userId)
        val validatedConnectionId =
          model.ConnectionId
            .from(request.connectionId)
            .fold(
              _ => Left(InvalidArgumentError("connectionId", "valid id", request.connectionId).logWarn),
              id => Right(id)
            )

        validatedConnectionId.toFutureEither
          .flatMap(connectionId => messages.getConnectionMessages(userId, connectionId))
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

      FutureEither(ConnectionId.from(request.connectionId))
        .mapLeft(_ => InvalidArgumentError("connectionId", "a valid UUID", request.connectionId))
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
      implicit val loggingContext = LoggingContext("request" -> request, "userId" -> userId)

      for {
        connectionId <-
          ConnectionId
            .from(request.connectionId)
            .fold(
              _ =>
                Future.failed(
                  InvalidArgumentError("connectionId", "a valid connectionId", request.connectionId).toStatus
                    .asRuntimeException()
                ),
              Future.successful
            )
        response <-
          messages
            .insertMessage(userId, connectionId, request.message.toByteArray)
            .wrapExceptions
            .successMap(_ => connector_api.SendMessageResponse())
      } yield response
    }

    authenticator.authenticated("sendMessage", request) { participantId =>
      f(participantId)
    }
  }

  override def getBuildInfo(request: connector_api.GetBuildInfoRequest): Future[connector_api.GetBuildInfoResponse] = {
    nodeService
      .getNodeBuildInfo(node_api.GetNodeBuildInfoRequest())
      .map(nodeBuildInfo =>
        connector_api
          .GetBuildInfoResponse()
          .withVersion(BuildInfo.version)
          .withScalaVersion(BuildInfo.scalaVersion)
          .withSbtVersion(BuildInfo.sbtVersion)
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
