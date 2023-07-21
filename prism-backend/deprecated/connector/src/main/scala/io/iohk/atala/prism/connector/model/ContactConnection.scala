package io.iohk.atala.prism.connector.model

final case class ContactConnection(
    connectionId: Option[ConnectionId],
    contactToken: Option[TokenString],
    connectionStatus: ConnectionStatus
)
