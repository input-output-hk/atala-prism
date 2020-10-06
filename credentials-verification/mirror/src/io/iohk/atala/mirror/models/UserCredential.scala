package io.iohk.atala.mirror.models

import java.time.Instant

import io.iohk.atala.mirror.models.Connection.ConnectionToken
import io.iohk.atala.mirror.models.UserCredential.{IssuersDID, MessageId, MessageReceivedDate, RawCredential}

case class UserCredential(
    connectionToken: ConnectionToken,
    rawCredential: RawCredential,
    issuersDID: Option[IssuersDID],
    messageId: MessageId,
    messageReceivedDate: MessageReceivedDate
)

object UserCredential {
  case class RawCredential(rawCredential: String) extends AnyVal

  case class IssuersDID(issuersDID: String) extends AnyVal

  case class MessageId(messageId: String) extends AnyVal

  case class MessageReceivedDate(date: Instant) extends AnyVal
}
