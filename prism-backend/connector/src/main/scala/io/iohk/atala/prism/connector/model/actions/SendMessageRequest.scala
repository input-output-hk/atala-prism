package io.iohk.atala.prism.connector.model.actions

import io.iohk.atala.prism.connector.model.{ConnectionId, MessageId}

case class SendMessageRequest(
    connectionId: ConnectionId,
    message: Array[Byte],
    id: Option[MessageId]
)
