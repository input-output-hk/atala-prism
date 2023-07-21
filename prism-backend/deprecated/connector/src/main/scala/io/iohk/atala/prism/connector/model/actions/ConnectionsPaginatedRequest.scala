package io.iohk.atala.prism.connector.model.actions

import io.iohk.atala.prism.connector.model.ConnectionId

case class ConnectionsPaginatedRequest(
    limit: Int,
    lastSeenConnectionId: Option[ConnectionId]
)
