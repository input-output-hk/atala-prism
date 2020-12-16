package io.iohk.atala.prism.kycbridge.models

import io.iohk.atala.prism.kycbridge.models.Connection.AcuantDocumentInstanceId
import io.iohk.atala.prism.models.{ConnectionId, ConnectionState, ConnectionToken}

case class Connection(
    token: ConnectionToken,
    id: Option[ConnectionId],
    state: ConnectionState,
    acuantDocumentInstanceId: Option[AcuantDocumentInstanceId]
)

object Connection {

  case class AcuantDocumentInstanceId(id: String) extends AnyVal
}
