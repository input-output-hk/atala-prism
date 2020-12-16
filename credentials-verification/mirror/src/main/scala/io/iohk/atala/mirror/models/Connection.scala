package io.iohk.atala.mirror.models

import io.iohk.atala.mirror.models.Connection._
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.{ConnectionId, ConnectionState, ConnectionToken}

case class Connection(
    token: ConnectionToken,
    id: Option[ConnectionId],
    state: ConnectionState,
    holderDID: Option[DID],
    payIdName: Option[PayIdName]
)

object Connection {

  case class PayIdName(name: String) extends AnyVal
}
