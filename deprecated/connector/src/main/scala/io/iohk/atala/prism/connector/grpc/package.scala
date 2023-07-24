package io.iohk.atala.prism.connector

import cats.data.NonEmptyList
import cats.implicits._
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeader
import io.iohk.atala.prism.connector.model.actions._
import io.iohk.atala.prism.connector.model.{ParticipantLogo, ParticipantType, TokenString, UpdateParticipantProfile}
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.identity.{CanonicalPrismDid, PrismDid}
import io.iohk.atala.prism.protos.connector_api
import io.iohk.atala.prism.protos.connector_api.RegisterDIDRequest.RegisterWith
import io.iohk.atala.prism.protos.connector_api.UpdateProfileRequest
import io.iohk.atala.prism.protos.connector_models.MessageToSendByConnectionToken
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation

import scala.util.{Failure, Success, Try}

package object grpc {
  implicit val participantProfileConverter: ProtoConverter[UpdateProfileRequest, UpdateParticipantProfile] = {
    (request, _) =>
      {
        for {
          name <- Try {
            if (request.name.trim.isEmpty)
              throw new RuntimeException("The name is required")
            else request.name.trim
          }

          logo <- Try {
            val bytes = request.logo.toByteArray
            if (bytes.isEmpty) None
            else Some(ParticipantLogo(bytes.toVector))
          }
        } yield UpdateParticipantProfile(name, logo)
      }
  }

  implicit val sendMessagesConverter: ProtoConverter[
    connector_api.SendMessagesRequest,
    SendMessagesRequest
  ] = { (request: connector_api.SendMessagesRequest, _) =>
    def parseMessage(
        message: MessageToSendByConnectionToken
    ): Try[SendMessagesRequest.MessageToSend] = {
      Utils
        .parseMessageId(message.id)
        .map(messageId =>
          SendMessagesRequest.MessageToSend(
            connectionToken = TokenString(message.connectionToken),
            message = message.message.fold(Array.empty[Byte])(_.toByteArray),
            id = messageId
          )
        )
    }

    request.messagesByConnectionToken
      .map(parseMessage)
      .toList
      .sequence
      .map(messages => SendMessagesRequest(NonEmptyList.fromList(messages)))

  }

  implicit val getConnectionByIdRequestConverter: ProtoConverter[
    connector_api.GetConnectionByIdRequest,
    GetConnectionByIdRequest
  ] =
    (in: connector_api.GetConnectionByIdRequest, _) => {
      model.ConnectionId
        .from(in.id)
        .fold(
          _ =>
            Failure(
              new IllegalArgumentException(
                s"Invalid value for id, expected valid id, got ${in.id}"
              )
            ),
          id => Success(GetConnectionByIdRequest(id))
        )

    }

  implicit val connectionsPaginatedRequestConverter: ProtoConverter[
    connector_api.GetConnectionsPaginatedRequest,
    ConnectionsPaginatedRequest
  ] =
    (in: connector_api.GetConnectionsPaginatedRequest, _) => {
      model.ConnectionId
        .optional(in.lastSeenConnectionId)
        .fold(
          _ =>
            Failure(
              new IllegalArgumentException(
                s"Invalid value for lastSeenConnectionId, expected valid id, got ${in.lastSeenConnectionId}"
              )
            ),
          maybeConnectionId => Success(ConnectionsPaginatedRequest(in.limit, maybeConnectionId))
        )

    }

  implicit val revokeConnectionRequestConverter: ProtoConverter[
    connector_api.RevokeConnectionRequest,
    RevokeConnectionRequest
  ] =
    (in: connector_api.RevokeConnectionRequest, _) =>
      Utils.parseConnectionId(in.connectionId).map(RevokeConnectionRequest)

  implicit val addConnectionFromTokenRequestConverter: ProtoConverter[
    connector_api.AddConnectionFromTokenRequest,
    AddConnectionRequest
  ] =
    (in: connector_api.AddConnectionFromTokenRequest, grpcHeader) => {
      lazy val publicKeyIsMissing = Failure(
        new RuntimeException(
          "The encoded public key is required to accept a connection"
        )
      )
      for {
        parsedKey <- Try(
          in.holderEncodedPublicKey.map(encodedKey => EC.toPublicKeyFromBytes(encodedKey.publicKey.toByteArray))
        )
        token = TokenString(in.token)
        _ = println(s"maybeHeader + $grpcHeader")
        basedOn <- grpcHeader match {
          case Some(
                header @ GrpcAuthenticationHeader.UnpublishedDIDBased(
                  _,
                  _,
                  _,
                  _
                )
              ) =>
            Success(
              UnpublishedDidBasedAddConnectionRequest(token, header).asLeft
            )
          case Some(
                header @ GrpcAuthenticationHeader.PublicKeyBased(_, _, _)
              ) =>
            parsedKey.fold[Try[Either[
              UnpublishedDidBasedAddConnectionRequest,
              PublicKeyBasedAddConnectionRequest
            ]]](
              publicKeyIsMissing
            )(publicKey =>
              Success(
                PublicKeyBasedAddConnectionRequest(
                  token,
                  publicKey,
                  header
                ).asRight
              )
            )
          case _ => publicKeyIsMissing
        }
      } yield AddConnectionRequest(token, basedOn)
    }

  implicit val registerDIDRequestConverter: ProtoConverter[connector_api.RegisterDIDRequest, RegisterDIDRequest] =
    (in: connector_api.RegisterDIDRequest, _) =>
      for {
        didOrCreateDidOperation <- parseRegisterWith(in.registerWith)
        tpe <- in.role match {
          case connector_api.RegisterDIDRequest.Role.issuer =>
            Success(ParticipantType.Issuer)
          case connector_api.RegisterDIDRequest.Role.verifier =>
            Success(ParticipantType.Verifier)
          case _ => Failure(new IllegalArgumentException("Unknown role"))
        }
        logo = ParticipantLogo(in.logo.toByteArray.toVector)
      } yield RegisterDIDRequest(in.name, didOrCreateDidOperation, tpe, logo)

  implicit val messagesPaginatedRequestConverter: ProtoConverter[
    connector_api.GetMessagesPaginatedRequest,
    MessagesPaginatedRequest
  ] =
    (in: connector_api.GetMessagesPaginatedRequest, _) =>
      Utils
        .getMessageIdField(in.lastSeenMessageId, "lastSeenMessageId")
        .map(MessagesPaginatedRequest(_, in.limit))

  implicit val messageStreamRequestConverter: ProtoConverter[
    connector_api.GetMessageStreamRequest,
    GetMessageStreamRequest
  ] =
    (in: connector_api.GetMessageStreamRequest, _) =>
      Utils
        .getMessageIdField(in.lastSeenMessageId, "lastSeenMessageId")
        .map(GetMessageStreamRequest)

  implicit val getMessagesForConnectionRequestConverter: ProtoConverter[
    connector_api.GetMessagesForConnectionRequest,
    GetMessagesForConnectionRequest
  ] =
    (in: connector_api.GetMessagesForConnectionRequest, _) =>
      Utils
        .parseConnectionId(in.connectionId)
        .map(GetMessagesForConnectionRequest)

  implicit val getConnectionCommunicationKeysRequestConverter: ProtoConverter[
    connector_api.GetConnectionCommunicationKeysRequest,
    GetConnectionCommunicationKeysRequest
  ] =
    (in: connector_api.GetConnectionCommunicationKeysRequest, _) =>
      Utils
        .parseConnectionId(in.connectionId)
        .map(GetConnectionCommunicationKeysRequest)

  implicit val sendMessageRequestConverter: ProtoConverter[connector_api.SendMessageRequest, SendMessageRequest] =
    (in: connector_api.SendMessageRequest, _) =>
      for {
        connectionId <- Utils.parseConnectionId(in.connectionId)
        messageId <- Utils.parseMessageId(in.id)
      } yield SendMessageRequest(
        connectionId,
        in.message.toByteArray,
        messageId
      )

  private def parseRegisterWith(
      in: connector_api.RegisterDIDRequest.RegisterWith
  ): Try[Either[PrismDid, SignedAtalaOperation]] =
    in match {
      case RegisterWith.CreateDidOperation(operation) =>
        Success(operation.asRight)
      case RegisterWith.ExistingDid(maybeDid) => parseCanonicalDid(maybeDid)
      case _ =>
        Failure(
          new IllegalArgumentException(
            "Expected existing DID or atala operation"
          )
        )
    }

  private def parseCanonicalDid(
      maybeDid: String
  ): Try[Either[PrismDid, SignedAtalaOperation]] =
    Try(
      PrismDid
        .fromString(maybeDid)
    ).toOption
      .fold[Try[Either[PrismDid, SignedAtalaOperation]]](
        Failure(new IllegalArgumentException("Invalid DID"))
      ) {
        case did: CanonicalPrismDid => Success(did.asLeft)
        case _ =>
          Failure(new IllegalArgumentException("Expected published did"))
      }

}
