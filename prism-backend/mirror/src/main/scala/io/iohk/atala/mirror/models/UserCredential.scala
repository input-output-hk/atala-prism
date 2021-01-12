package io.iohk.atala.mirror.models

import java.time.Instant

import enumeratum.{DoobieEnum, Enum, EnumEntry}
import io.iohk.atala.prism.models.{ConnectionToken, ConnectorMessageId}
import io.iohk.atala.mirror.models.UserCredential.{CredentialStatus, MessageReceivedDate, RawCredential}
import io.iohk.atala.prism.identity.DID

case class UserCredential(
    connectionToken: ConnectionToken,
    rawCredential: RawCredential,
    issuersDID: Option[DID],
    messageId: ConnectorMessageId,
    messageReceivedDate: MessageReceivedDate,
    status: CredentialStatus
)

object UserCredential {
  case class RawCredential(rawCredential: String) extends AnyVal

  case class MessageReceivedDate(date: Instant) extends AnyVal

  sealed abstract class CredentialStatus(value: String) extends EnumEntry {
    override def entryName: String = value
  }

  object CredentialStatus extends Enum[CredentialStatus] with DoobieEnum[CredentialStatus] {
    lazy val values = findValues

    final case object Received extends CredentialStatus("RECEIVED")
    final case object Valid extends CredentialStatus("VALID")
    final case object Invalid extends CredentialStatus("INVALID")
    final case object Revoked extends CredentialStatus("REVOKED")
  }
}
