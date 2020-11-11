package io.iohk.atala.mirror.models

import java.util.UUID

import enumeratum.{DoobieEnum, Enum, EnumEntry}
import io.iohk.atala.mirror.models.Connection._
import io.iohk.atala.prism.identity.DID

case class Connection(
    token: ConnectionToken,
    id: Option[ConnectionId],
    state: ConnectionState,
    holderDID: Option[DID]
)

object Connection {
  case class ConnectionToken(token: String) extends AnyVal
  case class ConnectionId(uuid: UUID) extends AnyVal

  sealed abstract class ConnectionState(value: String) extends EnumEntry {
    override def entryName: String = value
  }
  object ConnectionState extends Enum[ConnectionState] with DoobieEnum[ConnectionState] {
    lazy val values = findValues

    final case object Invited extends ConnectionState("INVITED")
    final case object Connected extends ConnectionState("CONNECTED")
    final case object Revoked extends ConnectionState("REVOKED")
  }
}
