package io.iohk.atala.mirror.models

import java.time.Instant

import enumeratum.{DoobieEnum, Enum, EnumEntry}
import io.iohk.atala.mirror.models.Connection.ConnectionToken
import io.iohk.atala.mirror.models.UserCredential.{
  CredentialStatus,
  IssuersDID,
  MessageId,
  MessageReceivedDate,
  RawCredential
}

case class UserCredential(
    connectionToken: ConnectionToken,
    rawCredential: RawCredential,
    issuersDID: Option[IssuersDID],
    messageId: MessageId,
    messageReceivedDate: MessageReceivedDate,
    status: CredentialStatus
)

object UserCredential {
  case class RawCredential(rawCredential: String) extends AnyVal

  case class IssuersDID(issuersDID: String) extends AnyVal

  case class MessageId(messageId: String) extends AnyVal

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
