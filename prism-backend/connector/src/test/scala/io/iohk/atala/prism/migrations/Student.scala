package io.iohk.atala.prism.migrations

import doobie.Meta
import enumeratum.{Enum, EnumEntry}

// kept for legacy models and db types
object Student {
  implicit val studentConnectionStatusMeta: Meta[Student.ConnectionStatus] =
    Meta[String].timap(Student.ConnectionStatus.withNameInsensitive)(
      _.entryName
    )

  sealed abstract class ConnectionStatus(value: String) extends EnumEntry {
    override def entryName: String = value
  }
  object ConnectionStatus extends Enum[ConnectionStatus] {
    lazy val values = findValues

    final case object InvitationMissing extends ConnectionStatus("INVITATION_MISSING")
    final case object ConnectionMissing extends ConnectionStatus("CONNECTION_MISSING")
    final case object ConnectionAccepted extends ConnectionStatus("CONNECTION_ACCEPTED")
    final case object ConnectionRevoked extends ConnectionStatus("CONNECTION_REVOKED")
  }
}
