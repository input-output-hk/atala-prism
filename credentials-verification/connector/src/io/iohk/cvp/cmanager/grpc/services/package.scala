package io.iohk.cvp.cmanager.grpc

import io.iohk.connector.UserIdInterceptor
import io.iohk.cvp.cmanager.models.Issuer

package object services {
  private[grpc] def getIssuerId(): Issuer.Id = {
    val participant =
      UserIdInterceptor.USER_ID_CTX_KEY.get().getOrElse(throw new RuntimeException("userId header missing"))
    Issuer.Id(participant.uuid)
  }
}
