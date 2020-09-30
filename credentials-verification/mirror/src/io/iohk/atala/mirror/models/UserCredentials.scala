package io.iohk.atala.mirror.models

import io.iohk.atala.mirror.models.Connection.ConnectionToken
import io.iohk.atala.mirror.models.UserCredentials.{IssuersDID, RawCredential}

case class UserCredentials(
    connectionToken: ConnectionToken,
    rawCredential: RawCredential,
    issuersDID: IssuersDID
)

object UserCredentials {
  case class RawCredential(rawCredential: String) extends AnyVal

  case class IssuersDID(issuersDID: String) extends AnyVal
}
