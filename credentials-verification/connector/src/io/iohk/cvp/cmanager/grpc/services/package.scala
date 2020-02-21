package io.iohk.cvp.cmanager.grpc

import io.grpc.Context
import io.iohk.cvp.cmanager.models.Issuer
import io.iohk.cvp.grpc.{GrpcAuthenticationHeader, GrpcAuthenticationHeaderParser}

package object services {

  /**
    * Get the issuer id associated to the current grpc context.
    *
    * NOTE: This method uses the [java.lang.ThreadLocal], be sure to call it on the grpc thread
    *       that's executing the request, otherwise, you may get the issuer id for another request.
    */
  private[grpc] def getIssuerId(): Issuer.Id = {
    GrpcAuthenticationHeaderParser.parse(Context.current()) match {
      case Some(GrpcAuthenticationHeader.Legacy(userId)) => Issuer.Id(userId.uuid)
      case _ => throw new RuntimeException("userId header missing, legacy authentication expected")
    }
  }
}
