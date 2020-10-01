package io.iohk.atala.mirror.models

import io.iohk.atala.mirror.models.Connection.ConnectionToken
import io.iohk.atala.mirror.models.UserCredential.{IssuersDID, RawCredential}

case class UserCredential(
    connectionToken: ConnectionToken,
    rawCredential: RawCredential,
    issuersDID: Option[IssuersDID]
)

object UserCredential {
  case class RawCredential(rawCredential: String) extends AnyVal

  case class IssuersDID(issuersDID: String) extends AnyVal
}
