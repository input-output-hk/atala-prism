package io.iohk.atala.prism.kycbridge.models

import java.time.Instant
import io.iohk.atala.prism.models.{ConnectionId, ConnectionState, ConnectionToken}
import io.iohk.atala.prism.kycbridge.models.Connection.AcuantDocumentInstanceId
import io.iohk.atala.prism.kycbridge.models.assureId.DocumentStatus

case class Connection(
    token: ConnectionToken,
    id: Option[ConnectionId],
    state: ConnectionState,
    updatedAt: Instant = Instant.now(),
    acuantDocumentInstanceId: Option[AcuantDocumentInstanceId],
    acuantDocumentStatus: Option[DocumentStatus]
)

object Connection {
  case class AcuantDocumentInstanceId(id: String) extends AnyVal
}
