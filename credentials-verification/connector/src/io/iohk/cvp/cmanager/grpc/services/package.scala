package io.iohk.cvp.cmanager.grpc

import io.iohk.cvp.cmanager.models.Issuer
import io.iohk.cvp.grpc.UserIdInterceptor

package object services {
  private[grpc] def getIssuerId(): Issuer.Id = {
    val participant = UserIdInterceptor.USER_ID_CTX_KEY.get()
    Issuer.Id(participant.uuid)
  }
}
