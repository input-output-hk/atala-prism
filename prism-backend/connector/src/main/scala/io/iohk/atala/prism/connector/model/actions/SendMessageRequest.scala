package io.iohk.atala.prism.connector.model.actions

import io.iohk.atala.prism.connector.model.ConnectionId

case class SendMessageRequest(connectionId: ConnectionId, message: Array[Byte])
