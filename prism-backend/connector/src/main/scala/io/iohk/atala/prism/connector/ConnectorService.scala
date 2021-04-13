package io.iohk.atala.prism.connector

import cats.effect._
import cats.syntax.functor._
import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import io.iohk.atala.prism.BuildInfo
import io.iohk.atala.prism.auth.AuthSupport
import io.iohk.atala.prism.auth.grpc.SignedRequestsHelper
import io.iohk.atala.prism.auth.utils.DIDUtils
import io.iohk.atala.prism.connector.errors._
import io.iohk.atala.prism.connector.grpc._
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.model.actions._
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository
import io.iohk.atala.prism.connector.services.{
  ConnectionsService,
  MessageNotificationService,
  MessagesService,
  RegistrationService
}
import io.iohk.atala.prism.crypto.{EC, ECPublicKey}
import io.iohk.atala.prism.models.{ParticipantId, ProtoCodecs}
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.connector_api.{GetMessageStreamResponse, UpdateProfileRequest, UpdateProfileResponse}
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.protos.{connector_api, connector_models, node_api}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ConnectorService(
    connections: ConnectionsService,
    messages: MessagesService,
    registrationService: RegistrationService,
    messageNotificationService: MessageNotificationService,
    val authenticator: ConnectorAuthenticator,
    nodeService: NodeServiceGrpc.NodeService,
    participantsRepository: ParticipantsRepository
)(implicit
    executionContext: ExecutionContext
) extends connector_api.ConnectorServiceGrpc.ConnectorService
    with ConnectorErrorSupport
    with AuthSupport[ConnectorError, ParticipantId] {

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
  ): Future[connector_api.GetConnectionByTokenResponse] =
    unitAuth("getConnectionByToken", request) { (_, _) =>
      connections
        .getConnectionByToken(new TokenString(request.token))
        .map(maybeConnection => connector_api.GetConnectionByTokenResponse(maybeConnection.map(_.toProto)))
    }

  /** Get active connections for current participant
    *
    * Available to: Holder, Issuer, Validator
    */
  override def getConnectionsPaginated(
      request: connector_api.GetConnectionsPaginatedRequest
  ): Future[connector_api.GetConnectionsPaginatedResponse] =
    auth[ConnectionsPaginatedRequest]("getConnectionsPaginated", request) {
      (participantId, connectionsPaginatedRequest) =>
        connections
          .getConnectionsPaginated(
            participantId,
            connectionsPaginatedRequest.limit,
            connectionsPaginatedRequest.lastSeenConnectionId
          )
          .map(conns => connector_api.GetConnectionsPaginatedResponse(conns.map(_.toProto)))
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
    unitPublic("getConnectionTokenInfo", request) { _ =>
      connections
        .getTokenInfo(new model.TokenString(request.token))
        .map { participantInfo =>
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

    def verifyRequestSignature(in: AddConnectionRequest): FutureEither[ConnectorError, Unit] =
      in.basedOn match {
        case Right(PublicKeyBasedAddConnectionRequest(_, publicKey, authHeader)) =>
          val payload = SignedRequestsHelper.merge(authHeader.requestNonce, request.toByteArray).toArray
          val resultEither = for {
            _ <- Either.cond(
              authHeader.publicKey == publicKey,
              (),
              InvalidArgumentError("publicKey", "key matching one in GRPC header", "different key")
            )
            _ <- Either.cond(
              EC.verify(payload, publicKey, authHeader.signature),
              (),
              SignatureVerificationError()
            )
          } yield ()
          Future.successful(resultEither).toFutureEither
        case Left(UnpublishedDidBasedAddConnectionRequest(_, authHeader)) =>
          val payload = SignedRequestsHelper.merge(authHeader.requestNonce, request.toByteArray).toArray
          for {
            didData <-
              DIDUtils
                .validateDid(authHeader.did)
                .mapLeft(_ => InvalidArgumentError("did", "valid unpublished did", "invalid"))
            publicKey <-
              DIDUtils
                .findPublicKey(didData, authHeader.keyId)
                .mapLeft(_ => InvalidArgumentError("did", "unpublished did with public key", "invalid"))
            _ <-
              Either
                .cond(EC.verify(payload, publicKey, authHeader.signature), (), SignatureVerificationError())
                .toFutureEither
          } yield ()
      }

    public[AddConnectionRequest]("addConnectionFromToken", request) { addConnectionRequest =>
      val result = for {
        _ <- verifyRequestSignature(addConnectionRequest)
        connectionCreationResult <-
          connections.addConnectionFromToken(addConnectionRequest.token, addConnectionRequest.didOrPublicKey)
      } yield connectionCreationResult

      result.map {
        case (userId, connectionInfo) =>
          connector_api
            .AddConnectionFromTokenResponse(Some(connectionInfo.toProto))
            .withUserId(userId.uuid.toString)
      }
    }
  }

  /** Delete active connection
    *
    * Available to: Holder, Issuer, Validator
    *
    * Errors:
    * Connection does not exist (UNKNOWN)
    */
  override def revokeConnection(
      request: connector_api.RevokeConnectionRequest
  ): Future[connector_api.RevokeConnectionResponse] =
    auth[RevokeConnectionRequest]("revokeConnection", request) { (participantId, revokeConnectionRequest) =>
      connections
        .revokeConnection(participantId, revokeConnectionRequest.connectionId)
        .as(connector_api.RevokeConnectionResponse())
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
  override def registerDID(request: connector_api.RegisterDIDRequest): Future[connector_api.RegisterDIDResponse] =
    public[RegisterDIDRequest]("registerDID", request) { registerDidRequest =>
      registrationService
        .register(
          registerDidRequest.tpe,
          registerDidRequest.name,
          registerDidRequest.logo,
          registerDidRequest.operation
        )
        .map { registerResult =>
          connector_api
            .RegisterDIDResponse(did = registerResult.did.value)
            .withTransactionInfo(ProtoCodecs.toTransactionInfo(registerResult.transactionInfo))
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
  ): Future[connector_api.GenerateConnectionTokenResponse] =
    unitAuth("generateConnectionToken", request) { (participantId, _) =>
      connections
        .generateTokens(participantId, if (request.count == 0) 1 else request.count)
        .map(tokenStrings => connector_api.GenerateConnectionTokenResponse(tokenStrings.map(_.token)))
    }

  /** Return messages received after given time moment, sorted in ascending order by receive time
    *
    * Available to: Issuer, Holder, Validator
    */
  override def getMessagesPaginated(
      request: connector_api.GetMessagesPaginatedRequest
  ): Future[connector_api.GetMessagesPaginatedResponse] = {
    auth[MessagesPaginatedRequest]("getMessagesPaginated", request) { (participantId, messagesPaginatedRequest) =>
      messages
        .getMessagesPaginated(
          participantId,
          messagesPaginatedRequest.limit,
          messagesPaginatedRequest.lastSeenMessageId
        )
        .map(msgs => connector_api.GetMessagesPaginatedResponse(msgs.map(_.toProto)))
    }
  }

  override def getMessageStream(
      request: connector_api.GetMessageStreamRequest,
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

    auth[GetMessageStreamRequest]("getMessageStream", request) { (participantId, getMessageStreamRequest) =>
      FutureEither.right(streamMessages(participantId, getMessageStreamRequest.lastSeenMessageId))
    }
    ()
  }

  override def getMessagesForConnection(
      request: connector_api.GetMessagesForConnectionRequest
  ): Future[connector_api.GetMessagesForConnectionResponse] =
    auth[GetMessagesForConnectionRequest]("getMessagesForConnection", request) {
      (participantId, getMessagesForConnectionRequest) =>
        messages
          .getConnectionMessages(participantId, getMessagesForConnectionRequest.connectionId)
          .map(msgs => connector_api.GetMessagesForConnectionResponse(msgs.map(_.toProto)))
    }

  /** Returns public keys that can be used for secure communication with the other end of connection
    */
  override def getConnectionCommunicationKeys(
      request: connector_api.GetConnectionCommunicationKeysRequest
  ): Future[connector_api.GetConnectionCommunicationKeysResponse] = {

    def toResponse(keys: Seq[(String, ECPublicKey)]) =
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

    auth[GetConnectionCommunicationKeysRequest]("getConnectionCommunicationKeys", request) {
      (participantId, getConnectionCommunicationKeysRequest) =>
        connections
          .getConnectionCommunicationKeys(getConnectionCommunicationKeysRequest.connectionId, participantId)
          .map(toResponse)
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
  override def sendMessage(request: connector_api.SendMessageRequest): Future[connector_api.SendMessageResponse] =
    auth[SendMessageRequest]("sendMessage", request) { (participantId, sendMessageRequest) =>
      messages
        .insertMessage(participantId, sendMessageRequest.connectionId, sendMessageRequest.message)
        .as(connector_api.SendMessageResponse())
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
  ): Future[connector_api.GetCurrentUserResponse] =
    unitAuth("getCurrentUser", request) { (participantId, _) =>
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
    }

  override def updateParticipantProfile(
      request: UpdateProfileRequest
  ): Future[UpdateProfileResponse] =
    auth[UpdateParticipantProfile]("updateParticipantProfile", request) { (participantId, updateProfile) =>
      participantsRepository
        .updateParticipantProfileBy(participantId, updateProfile)
        .as(connector_api.UpdateProfileResponse())
    }

  /** Send messages over many connections
    *
    * Available to: Issuer, Holder, Validator
    *
    * Errors:
    * Unknown connection (UNKNOWN)
    * Connection closed (FAILED_PRECONDITION)
    */
  override def sendMessages(request: connector_api.SendMessagesRequest): Future[connector_api.SendMessagesResponse] =
    auth[SendMessages]("sendMessages", request) { (participantId, query) =>
      query.messages.fold(
        FutureEither.right[ConnectorError, connector_api.SendMessagesResponse](connector_api.SendMessagesResponse())
      ) { messagesToInsert =>
        messages
          .insertMessages(participantId, messagesToInsert)
          .as(connector_api.SendMessagesResponse())
      }
    }

}
