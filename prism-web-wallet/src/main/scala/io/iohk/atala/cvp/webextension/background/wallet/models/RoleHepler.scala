package io.iohk.atala.cvp.webextension.background.wallet.models

import io.iohk.atala.cvp.webextension.common.models.Role
import io.iohk.atala.cvp.webextension.common.models.Role.{Issuer, Verifier}
import io.iohk.atala.prism.protos.connector_api.{GetCurrentUserResponse, RegisterDIDRequest}

object RoleHepler {
  def toConnectorApiRole(role: Role): RegisterDIDRequest.Role = {
    role match {
      case Issuer => RegisterDIDRequest.Role.issuer
      case Verifier => RegisterDIDRequest.Role.verifier
    }
  }

  def toRole(role: GetCurrentUserResponse.Role): Role = {
    role match {
      case GetCurrentUserResponse.Role.issuer => Issuer
      case GetCurrentUserResponse.Role.verifier => Verifier
      case GetCurrentUserResponse.Role.Unrecognized(roleValue) =>
        throw new IllegalArgumentException(s"Unrecognized role $roleValue")
    }
  }
}
