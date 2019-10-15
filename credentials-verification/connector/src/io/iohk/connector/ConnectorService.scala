package io.iohk.connector

import java.time.Instant
import java.util.UUID

import io.iohk.connector.protos._
import io.iohk.connector.services.{ConnectionsService, MessagesService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ConnectorService(connections: ConnectionsService, messages: MessagesService)(
    implicit executionContext: ExecutionContext
) extends ConnectorServiceGrpc.ConnectorService {

  /** Get active connections for current participant
    *
    * Available to: Holder, Issuer, Validator
    */
  override def getConnectionsPaginated(
      request: GetConnectionsPaginatedRequest
  ): Future[GetConnectionsPaginatedResponse] = {
    val userId = UserIdInterceptor.USER_ID_CTX_KEY.get()

    // TODO: Validate arguments, empty string should be considered as a non-present field
    // The limit validations throws a generic exception instead of a specific error.
    val lastSeenConnectionId = Try(request.lastSeenConnectionId)
      .map(UUID.fromString)
      .map(model.ConnectionId.apply)
      .toOption
    require(request.limit > 0, "The limit must be > 0")

    connections
      .getConnectionsPaginated(userId, request.limit, lastSeenConnectionId)
      .value
      .flatMap {
        case Left(err) => Future.failed(new Exception(s"Problem: $err"))
        case Right(connections) =>
          Future.successful(GetConnectionsPaginatedResponse(connections.map(_.toProto)))
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
    connections.getTokenInfo(new model.TokenString(request.token)).value.flatMap {
      case Left(err) => Future.failed(new Exception(s"Problem: $err"))
      case Right(participantInfo) => Future.successful(GetConnectionTokenInfoResponse(participantInfo.toProto))
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
    val userId = UserIdInterceptor.USER_ID_CTX_KEY.get()

    connections.addConnectionFromToken(userId, new model.TokenString(request.token)).value.flatMap {
      case Left(err) => Future.failed(new Exception(s"Problem: $err"))
      case Right(connectionInfo) => Future.successful(AddConnectionFromTokenResponse(connectionInfo.toProto))
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
    val userId = UserIdInterceptor.USER_ID_CTX_KEY.get()

    connections.generateToken(userId).value.flatMap {
      case Left(err) => Future.failed(new Exception(s"Problem: $err"))
      case Right(tokenString) => Future.successful(GenerateConnectionTokenResponse(tokenString.token))
    }
  }

  /** Return messages received after given time moment, sorted in ascending order by receive time
    *
    * Available to: Issuer, Holder, Validator
    */
  override def getMessagesPaginated(request: GetMessagesPaginatedRequest): Future[GetMessagesPaginatedResponse] = {
    val userId = UserIdInterceptor.USER_ID_CTX_KEY.get()

    // TODO: Validate arguments, empty string should be considered as a non-present field
    // The limit validations throws a generic exception instead of a specific error.
    val lastSeenMessageId = Try(request.lastSeenMessageId)
      .map(UUID.fromString)
      .map(model.MessageId.apply)
      .toOption
    require(request.limit > 0, "The limit must be greater than zero")

    messages.getMessagesPaginated(userId, request.limit, lastSeenMessageId).value.flatMap {
      case Left(error) => Future.failed(new Exception(s"Problem: $error"))
      case Right(messagesSeq) => Future.successful(GetMessagesPaginatedResponse(messagesSeq.map(_.toProto)))
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
    val userId = UserIdInterceptor.USER_ID_CTX_KEY.get()
    val connectionId = model.ConnectionId(UUID.fromString(request.connectionId))

    messages.insertMessage(userId, connectionId, request.message.toByteArray).value.flatMap {
      case Left(error) => Future.failed(new Exception(s"Problem: $error"))
      case Right(_) => Future.successful(SendMessageResponse())
    }
  }
}
