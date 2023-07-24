package io.iohk.atala.prism.connector.grpc

import io.iohk.atala.prism.connector.model.{ConnectionStatus, ContactConnection}
import io.iohk.atala.prism.protos.{connector_models, console_models}
import io.scalaland.chimney.Transformer

object ProtoCodecs {
  implicit val contactConnectionStatus2Proto: Transformer[
    ConnectionStatus,
    console_models.ContactConnectionStatus
  ] = {
    case ConnectionStatus.InvitationMissing =>
      console_models.ContactConnectionStatus.STATUS_INVITATION_MISSING
    case ConnectionStatus.ConnectionMissing =>
      console_models.ContactConnectionStatus.STATUS_CONNECTION_MISSING
    case ConnectionStatus.ConnectionAccepted =>
      console_models.ContactConnectionStatus.STATUS_CONNECTION_ACCEPTED
    case ConnectionStatus.ConnectionRevoked =>
      console_models.ContactConnectionStatus.STATUS_CONNECTION_REVOKED
  }

  implicit val contactConnection2Proto: Transformer[ContactConnection, connector_models.ContactConnection] =
    contactConnection => {
      connector_models.ContactConnection(
        connectionId = contactConnection.connectionId.map(_.toString).getOrElse(""),
        connectionToken = contactConnection.contactToken.map(_.token).getOrElse(""),
        connectionStatus = contactConnectionStatus2Proto.transform(
          contactConnection.connectionStatus
        )
      )
    }
}
