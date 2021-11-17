package io.iohk.atala.prism.connector

import cats.effect.unsafe.IORuntime
import cats.implicits.catsSyntaxEitherId
import cats.syntax.functor._
import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import io.iohk.atala.prism.BuildInfo
import io.iohk.atala.prism.auth.grpc.SignedRequestsHelper
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser._
import io.iohk.atala.prism.tracing.Tracing._
import io.iohk.atala.prism.auth.utils.DIDUtils
import io.iohk.atala.prism.auth.{AuthAndMiddlewareSupportF, AuthenticatorF}
import io.iohk.atala.prism.connector.errors._
import io.iohk.atala.prism.connector.grpc._
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.model.actions._
import io.iohk.atala.prism.connector.repositories.ConnectionsRepository.AddConnectionFromTokenError
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository
import io.iohk.atala.prism.connector.services.{
  ConnectionsService,
  MessageNotificationService,
  MessagesService,
  RegistrationService
}
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.metrics.RequestMeasureUtil.measureRequestFuture
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.protos.{common_models, connector_api, connector_models, node_api}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import org.slf4j.{Logger, LoggerFactory}
import shapeless.:+:

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ConnectorService(
    connections: ConnectionsService[IOWithTraceIdContext],
    messages: MessagesService[
      fs2.Stream[IOWithTraceIdContext, *],
      IOWithTraceIdContext
    ],
    registrationService: RegistrationService[IOWithTraceIdContext],
    messageNotificationService: MessageNotificationService,
    val authenticator: AuthenticatorF[ParticipantId, IOWithTraceIdContext],
    nodeService: NodeServiceGrpc.NodeService,
    participantsRepository: ParticipantsRepository[IOWithTraceIdContext]
)(implicit
    executionContext: ExecutionContext,
    runtime: IORuntime
) extends connector_api.ConnectorServiceGrpc.ConnectorService
    with ConnectorErrorSupport
    with AuthAndMiddlewareSupportF[ConnectorError, ParticipantId] {

  override protected val serviceName: String = "connector-service"
  override val IOruntime: IORuntime = runtime

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def healthCheck(
      request: common_models.HealthCheckRequest
  ): Future[common_models.HealthCheckResponse] =
    measureRequestFuture(serviceName, "healthCheck")(
      Future.successful(common_models.HealthCheckResponse())
    )

  /** Retrieve a connection for a given connection token.
    *
    * Available to: Holder, Issuer, Validator
    */
  override def getConnectionByToken(
      request: connector_api.GetConnectionByTokenRequest
  ): Future[connector_api.GetConnectionByTokenResponse] = {
    trace { traceId =>
      grpcHeader { header =>
        unitAuth("getConnectionByToken", request, header, traceId) { (_, _) =>
          connections
            .getConnectionByToken(new TokenString(request.token))
            .map(maybeConnection =>
              connector_api.GetConnectionByTokenResponse(
                maybeConnection.map(_.toProto)
              )
            )
            .run(traceId)
            .unsafeToFuture()
            .lift[ConnectorError]
        }
      }
    }
  }

  override def getConnectionById(
      request: connector_api.GetConnectionByIdRequest
  ): Future[connector_api.GetConnectionByIdResponse] = {
    trace { traceId =>
      grpcHeader { header =>
        auth[GetConnectionByIdRequest]("getConnectionById", request, header, traceId) { (participantId, typedRequest) =>
          connections
            .getConnectionById(participantId, typedRequest.id)
            .map(maybeConnection =>
              connector_api.GetConnectionByIdResponse(
                maybeConnection.map(_.toProto)
              )
            )
            .run(traceId)
            .unsafeToFuture()
            .lift[ConnectorError]
        }
      }
    }
  }

  /** Get active connections for current participant
    *
    * Available to: Holder, Issuer, Validator
    */
  override def getConnectionsPaginated(
      request: connector_api.GetConnectionsPaginatedRequest
  ): Future[connector_api.GetConnectionsPaginatedResponse] =
    trace { traceId =>
      grpcHeader { header =>
        auth[ConnectionsPaginatedRequest]("getConnectionsPaginated", request, header, traceId) {
          (participantId, connectionsPaginatedRequest) =>
            connections
              .getConnectionsPaginated(
                participantId,
                connectionsPaginatedRequest.limit,
                connectionsPaginatedRequest.lastSeenConnectionId
              )
              .run(traceId)
              .unsafeToFuture()
              .toFutureEither
              .mapLeft(_.unify)
              .map(conns => connector_api.GetConnectionsPaginatedResponse(conns.map(_.toProto)))
        }
      }
    }

  /** Return info about connection token such as creator info
    *
    * Available to: Holder
    *
    * Errors: Token does not exist (UNKNOWN)
    */
  override def getConnectionTokenInfo(
      request: connector_api.GetConnectionTokenInfoRequest
  ): Future[connector_api.GetConnectionTokenInfoResponse] = {
    trace { traceId =>
      unitPublic("getConnectionTokenInfo", request, None, traceId) { _ =>
        connections
          .getTokenInfo(new model.TokenString(request.token))
          .run(traceId)
          .unsafeToFuture()
          .toFutureEither
          .mapLeft(_.unify)
          .map { participantInfo =>
            connector_api.GetConnectionTokenInfoResponse(
              creatorName = participantInfo.name,
              creatorLogo = ByteString.copyFrom(
                participantInfo.logo.map(_.bytes).getOrElse(Vector.empty).toArray
              ),
              creatorDid = participantInfo.did.map(_.getValue).getOrElse("")
            )
          }
      }
    }
  }

  /** Instantiate connection from connection token
    *
    * Available to: Holder
    *
    * Errors: Token does not exist (UNKNOWN)
    */
  override def addConnectionFromToken(
      request: connector_api.AddConnectionFromTokenRequest
  ): Future[connector_api.AddConnectionFromTokenResponse] = {

    // TODO Maybe separate different InvalidArgumentError to different subtypes?
    // Here we just extend AddConnectionFromTokenError with errors from verifyRequestSignature
    // instead having VerifyRequestSignatureError to simplify types due to reduction of coproduct conversions
    type AddConnectionFromTokenFullError =
      InvalidArgumentError :+: SignatureVerificationError :+: AddConnectionFromTokenError

    def verifyRequestSignature(
        in: AddConnectionRequest
    ): FutureEither[AddConnectionFromTokenFullError, Unit] =
      in.basedOn match {
        case Right(
              PublicKeyBasedAddConnectionRequest(_, publicKey, authHeader)
            ) =>
          val payload = SignedRequestsHelper
            .merge(authHeader.requestNonce, request.toByteArray)
            .toArray
          val resultEither: Either[AddConnectionFromTokenFullError, Unit] =
            for {
              _ <- Either.cond(
                authHeader.publicKey == publicKey,
                (),
                co(
                  InvalidArgumentError(
                    "publicKey",
                    "key matching one in GRPC header",
                    "different key"
                  )
                )
              )
              _ <- Either.cond(
                EC.verifyBytes(payload, publicKey, authHeader.signature),
                (),
                co[AddConnectionFromTokenFullError](
                  SignatureVerificationError()
                )
              )
            } yield ()
          Future.successful(resultEither).toFutureEither
        case Left(UnpublishedDidBasedAddConnectionRequest(_, authHeader)) =>
          val payload = SignedRequestsHelper
            .merge(authHeader.requestNonce, request.toByteArray)
            .toArray
          for {
            didData <-
              DIDUtils
                .validateDid(authHeader.did)
                .mapLeft(_ =>
                  co(
                    InvalidArgumentError(
                      "did",
                      "valid unpublished did",
                      "invalid"
                    )
                  )
                )
            publicKey <-
              DIDUtils
                .findPublicKey(didData, authHeader.keyId)
                .mapLeft(_ =>
                  co(
                    InvalidArgumentError(
                      "did",
                      "unpublished did with public key",
                      "invalid"
                    )
                  )
                )
            _ <-
              Either
                .cond(
                  EC.verifyBytes(payload, publicKey, authHeader.signature),
                  (),
                  co[AddConnectionFromTokenFullError](
                    SignatureVerificationError()
                  )
                )
                .toFutureEither
          } yield ()
      }

    trace { traceId =>
      grpcHeader { header =>
        publicCo[AddConnectionRequest]("addConnectionFromToken", request, header, traceId) { addConnectionRequest =>
          for {
            _ <- verifyRequestSignature(addConnectionRequest)
            connectionInfo <- connections
              .addConnectionFromToken(
                addConnectionRequest.token,
                addConnectionRequest.didOrPublicKey
              )
              .run(traceId)
              .unsafeToFuture()
              .toFutureEither
              .mapLeft(_.embed[AddConnectionFromTokenFullError])
          } yield {
            println(connectionInfo)
            connector_api
              .AddConnectionFromTokenResponse(Some(connectionInfo.toProto))
          }
        }
      }
    }
  }

  /** Delete active connection
    *
    * Available to: Holder, Issuer, Validator
    *
    * Errors: Connection does not exist (UNKNOWN)
    */
  override def revokeConnection(
      request: connector_api.RevokeConnectionRequest
  ): Future[connector_api.RevokeConnectionResponse] = {
    trace { traceId =>
      grpcHeader { header =>
        authCo[RevokeConnectionRequest]("revokeConnection", request, header, traceId) {
          (participantId, revokeConnectionRequest) =>
            connections
              .revokeConnection(participantId, revokeConnectionRequest.connectionId)
              .run(traceId)
              .unsafeToFuture()
              .toFutureEither
              .as(connector_api.RevokeConnectionResponse())
        }
      }
    }
  }

  /** Bind DID to issuer
    *
    * Available to: Issuer
    *
    * Errors: Invalid DID (INVALID_ARGUMENT) Invalid DID document (INVALID_ARGUMENT) DID Document does not match DID
    * (INVALID_ARGUMENT)
    */
  override def registerDID(
      request: connector_api.RegisterDIDRequest
  ): Future[connector_api.RegisterDIDResponse] = {
    trace { traceId =>
      public[RegisterDIDRequest]("registerDID", request, None, traceId) { registerDidRequest =>
        registrationService
          .register(
            registerDidRequest.tpe,
            registerDidRequest.name,
            registerDidRequest.logo,
            registerDidRequest.didOrOperation
          )
          .run(traceId)
          .unsafeToFuture()
          .toFutureEither
          .mapLeft(_.unify)
          .map { registerResult =>
            val response = connector_api
              .RegisterDIDResponse(
                did = registerResult.did.getValue
              )
            registerResult.operationId
              .map(_.toProtoByteString)
              .fold(response)(response.withOperationId)
          }
      }
    }
  }

  /** Generate connection token that can be used to instantiate connection
    *
    * Available to: Issuer, Validator
    *
    * Errors: Billing plan doesn't allow token generation (PERMISSION_DENIED)
    */
  override def generateConnectionToken(
      request: connector_api.GenerateConnectionTokenRequest
  ): Future[connector_api.GenerateConnectionTokenResponse] = {
    trace { traceId =>
      grpcHeader { header =>
        unitAuth("generateConnectionToken", request, header, traceId) { (participantId, _) =>
          connections
            .generateTokens(
              participantId,
              if (request.count == 0) 1 else request.count
            )
            .run(traceId)
            .unsafeToFuture()
            .lift
            .map(tokenStrings =>
              connector_api.GenerateConnectionTokenResponse(
                tokenStrings.map(_.token)
              )
            )
        }
      }
    }
  }

  /** Return messages received after given time moment, sorted in ascending order by receive time
    *
    * Available to: Issuer, Holder, Validator
    */
  override def getMessagesPaginated(
      request: connector_api.GetMessagesPaginatedRequest
  ): Future[connector_api.GetMessagesPaginatedResponse] =
    trace { traceId =>
      grpcHeader { header =>
        auth[MessagesPaginatedRequest]("getMessagesPaginated", request, header, traceId) {
          (participantId, messagesPaginatedRequest) =>
            messages
              .getMessagesPaginated(
                participantId,
                messagesPaginatedRequest.limit,
                messagesPaginatedRequest.lastSeenMessageId
              )
              .run(traceId)
              .unsafeToFuture()
              .toFutureEither
              .mapLeft(_.unify)
              .map(msgs => connector_api.GetMessagesPaginatedResponse(msgs.map(_.toProto)))
        }
      }
    }

  override def getMessageStream(
      request: connector_api.GetMessageStreamRequest,
      responseObserver: StreamObserver[connector_api.GetMessageStreamResponse]
  ): Unit = {
    def streamMessages(
        recipientId: ParticipantId,
        lastSeenMessageId: Option[MessageId],
        traceId: TraceId
    ): Unit = {
      val existingMessageStream =
        messages.getMessageStream(
          recipientId = recipientId,
          lastSeenMessageId = lastSeenMessageId
        )
      val newMessageStream = messageNotificationService
        .stream(recipientId)
        .translate(TraceId.liftToIOWithTraceId)
      (existingMessageStream ++ newMessageStream)
        .map(message =>
          responseObserver.onNext(
            connector_api
              .GetMessageStreamResponse()
              .withMessage(message.toProto)
          )
        )
        .compile
        .drain
        .run(traceId)
        .unsafeToFuture()
        .onComplete {
          case Success(_) => responseObserver.onCompleted()
          case Failure(exception) =>
            logger.warn(
              s"Could not stream messages for recipient $recipientId",
              exception
            )
            responseObserver.onError(exception)
        }
    }

    trace { traceId =>
      grpcHeader { header =>
        auth[GetMessageStreamRequest]("getMessageStream", request, header, traceId) {
          (participantId, getMessageStreamRequest) =>
            FutureEither.right(
              streamMessages(
                participantId,
                getMessageStreamRequest.lastSeenMessageId,
                traceId
              )
            )
        }
      }
    }
    ()
  }

  override def getMessagesForConnection(
      request: connector_api.GetMessagesForConnectionRequest
  ): Future[connector_api.GetMessagesForConnectionResponse] = trace { traceId =>
    grpcHeader { header =>
      auth[GetMessagesForConnectionRequest]("getMessagesForConnection", request, header, traceId) {
        (participantId, getMessagesForConnectionRequest) =>
          messages
            .getConnectionMessages(
              participantId,
              getMessagesForConnectionRequest.connectionId
            )
            .map(msgs => connector_api.GetMessagesForConnectionResponse(msgs.map(_.toProto)))
            .run(traceId)
            .unsafeToFuture()
            .map(_.asRight)
            .toFutureEither
      }
    }
  }

  /** Returns public keys that can be used for secure communication with the other end of connection
    */
  override def getConnectionCommunicationKeys(
      request: connector_api.GetConnectionCommunicationKeysRequest
  ): Future[connector_api.GetConnectionCommunicationKeysResponse] = {

    def toResponse(keys: Seq[(String, ECPublicKey)]) =
      connector_api.GetConnectionCommunicationKeysResponse(
        keys = keys.map { case (keyId, key) =>
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

    trace { traceId =>
      grpcHeader { header =>
        auth[GetConnectionCommunicationKeysRequest](
          "getConnectionCommunicationKeys",
          request,
          header,
          traceId
        ) { (participantId, getConnectionCommunicationKeysRequest) =>
          connections
            .getConnectionCommunicationKeys(
              getConnectionCommunicationKeysRequest.connectionId,
              participantId
            )
            .run(traceId)
            .unsafeToFuture()
            .toFutureEither
            .mapLeft(_.unify)
            .map(toResponse)
        }
      }
    }
  }

  /** Send message over a connection
    *
    * Available to: Issuer, Holder, Validator
    *
    * Errors: Unknown connection (UNKNOWN) Connection closed (FAILED_PRECONDITION)
    */
  override def sendMessage(
      request: connector_api.SendMessageRequest
  ): Future[connector_api.SendMessageResponse] =
    trace { traceId =>
      grpcHeader { header =>
        authCo[SendMessageRequest]("sendMessage", request, header, traceId) { (participantId, sendMessageRequest) =>
          messages
            .insertMessage(
              sender = participantId,
              connection = sendMessageRequest.connectionId,
              content = sendMessageRequest.message,
              messageId = sendMessageRequest.id
            )
            .run(traceId)
            .unsafeToFuture()
            .toFutureEither
            .map(messageId => connector_api.SendMessageResponse(id = messageId.uuid.toString))
        }
      }
    }

  override def getBuildInfo(
      request: connector_api.GetBuildInfoRequest
  ): Future[connector_api.GetBuildInfoResponse] = {
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
  ): Future[connector_api.GetCurrentUserResponse] =
    trace { traceId =>
      grpcHeader { header =>
        unitAuth("getCurrentUser", request, header, traceId) { (participantId, _) =>
          participantsRepository
            .findBy(participantId)
            .run(traceId)
            .unsafeToFuture()
            .toFutureEither
            .mapLeft(_.unify)
            .map { info =>
              val role = info.tpe match {
                case ParticipantType.Holder =>
                  throw new NotImplementedError(
                    "This method is not available for holders right now"
                  )

                case ParticipantType.Issuer =>
                  connector_api.GetCurrentUserResponse.Role.issuer
                case ParticipantType.Verifier =>
                  connector_api.GetCurrentUserResponse.Role.verifier
              }
              val logo = info.logo.map(_.bytes).getOrElse(Vector.empty).toArray
              connector_api
                .GetCurrentUserResponse()
                .withName(info.name)
                .withRole(role)
                .withLogo(ByteString.copyFrom(logo))
            }
        }
      }
    }

  override def updateParticipantProfile(
      request: connector_api.UpdateProfileRequest
  ): Future[connector_api.UpdateProfileResponse] =
    trace { traceId =>
      grpcHeader { header =>
        auth[UpdateParticipantProfile]("updateParticipantProfile", request, header, traceId) {
          (participantId, updateProfile) =>
            participantsRepository
              .updateParticipantProfileBy(participantId, updateProfile)
              .run(traceId)
              .unsafeToFuture()
              .as(connector_api.UpdateProfileResponse())
              .map(_.asRight)
              .toFutureEither
        }
      }
    }

  /** Send messages over many connections
    *
    * Available to: Issuer, Holder, Validator
    *
    * Errors: Unknown connection (UNKNOWN) Connection closed (FAILED_PRECONDITION)
    */
  override def sendMessages(
      request: connector_api.SendMessagesRequest
  ): Future[connector_api.SendMessagesResponse] =
    trace { traceId =>
      grpcHeader { header =>
        auth[SendMessagesRequest]("sendMessages", request, header, traceId) { (participantId, query) =>
          query.messages.fold(
            FutureEither
              .right[ConnectorError, connector_api.SendMessagesResponse](
                connector_api.SendMessagesResponse()
              )
          ) { messagesToInsert =>
            messages
              .insertMessages(participantId, messagesToInsert)
              .run(traceId)
              .unsafeToFuture()
              .toFutureEither
              .mapLeft(_.unify)
              .map(messageIds =>
                connector_api
                  .SendMessagesResponse(ids = messageIds.map(_.uuid.toString))
              )
          }
        }
      }
    }
}
