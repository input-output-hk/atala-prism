package io.iohk.atala.prism.connector

import cats.data.NonEmptyList
import io.grpc.Context
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.connector.model.actions._
import io.iohk.atala.prism.connector.model.{
  ParticipantLogo,
  ParticipantType,
  SendMessages,
  TokenString,
  UpdateParticipantProfile
}
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.protos.connector_api.UpdateProfileRequest

import scala.util.{Failure, Success, Try}
import io.iohk.atala.prism.protos.connector_api
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation

package object grpc {
  implicit val participantProfileConverter: ProtoConverter[UpdateProfileRequest, UpdateParticipantProfile] = {
    request =>
      {
        for {
          name <- Try {
            if (request.name.trim.isEmpty) throw new RuntimeException("The name is required")
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

  implicit val sendMessagesConverter: ProtoConverter[connector_api.SendMessagesRequest, SendMessages] = {
    (request: connector_api.SendMessagesRequest) =>
      Try(SendMessages(NonEmptyList.fromList(request.messagesByConnectionToken.toList)))
  }

  implicit val connectionsPaginatedRequestConverter
      : ProtoConverter[connector_api.GetConnectionsPaginatedRequest, ConnectionsPaginatedRequest] =
    (in: connector_api.GetConnectionsPaginatedRequest) => {
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

  implicit val revokeConnectionRequestConverter
      : ProtoConverter[connector_api.RevokeConnectionRequest, RevokeConnectionRequest] =
    (in: connector_api.RevokeConnectionRequest) => Utils.parseConnectionId(in.connectionId).map(RevokeConnectionRequest)

  implicit val addConnectionFromTokenRequestConverter
      : ProtoConverter[connector_api.AddConnectionFromTokenRequest, AddConnectionRequest] =
    (in: connector_api.AddConnectionFromTokenRequest) =>
      Try(
        in.holderEncodedPublicKey
          .map { encodedKey =>
            EC.toPublicKey(encodedKey.publicKey.toByteArray)
          }
          .getOrElse(throw new RuntimeException("The encoded public key is required to accept a connection"))
      ).map { publicKey =>
        val maybeHeader = GrpcAuthenticationHeaderParser.parse(Context.current())
        AddConnectionRequest(TokenString(in.token), publicKey, maybeHeader)
      }

  implicit val registerDIDRequestConverter: ProtoConverter[connector_api.RegisterDIDRequest, RegisterDIDRequest] =
    (in: connector_api.RegisterDIDRequest) =>
      for {
        createDIDOperation <- in.createDIDOperation.fold[Try[SignedAtalaOperation]](
          Failure(new IllegalArgumentException("The createDIDOperation is mandatory"))
        )(Success(_))
        tpe <- in.role match {
          case connector_api.RegisterDIDRequest.Role.issuer => Success(ParticipantType.Issuer)
          case connector_api.RegisterDIDRequest.Role.verifier => Success(ParticipantType.Verifier)
          case _ => Failure(new IllegalArgumentException("Unknown role"))
        }
        logo = ParticipantLogo(in.logo.toByteArray.toVector)
      } yield RegisterDIDRequest(in.name, createDIDOperation, tpe, logo)

  implicit val messagesPaginatedRequestConverter
      : ProtoConverter[connector_api.GetMessagesPaginatedRequest, MessagesPaginatedRequest] =
    (in: connector_api.GetMessagesPaginatedRequest) =>
      Utils
        .getMessageIdField(in.lastSeenMessageId, "lastSeenMessageId")
        .map(MessagesPaginatedRequest(_, in.limit))

  implicit val messageStreamRequestConverter
      : ProtoConverter[connector_api.GetMessageStreamRequest, GetMessageStreamRequest] =
    (in: connector_api.GetMessageStreamRequest) =>
      Utils
        .getMessageIdField(in.lastSeenMessageId, "lastSeenMessageId")
        .map(GetMessageStreamRequest)

  implicit val getMessagesForConnectionRequestConverter
      : ProtoConverter[connector_api.GetMessagesForConnectionRequest, GetMessagesForConnectionRequest] =
    (in: connector_api.GetMessagesForConnectionRequest) =>
      Utils.parseConnectionId(in.connectionId).map(GetMessagesForConnectionRequest)

  implicit val getConnectionCommunicationKeysRequestConverter
      : ProtoConverter[connector_api.GetConnectionCommunicationKeysRequest, GetConnectionCommunicationKeysRequest] =
    (in: connector_api.GetConnectionCommunicationKeysRequest) =>
      Utils.parseConnectionId(in.connectionId).map(GetConnectionCommunicationKeysRequest)

  implicit val sendMessageRequestConverter: ProtoConverter[connector_api.SendMessageRequest, SendMessageRequest] =
    (in: connector_api.SendMessageRequest) =>
      Utils.parseConnectionId(in.connectionId).map(SendMessageRequest(_, in.message.toByteArray))

}
