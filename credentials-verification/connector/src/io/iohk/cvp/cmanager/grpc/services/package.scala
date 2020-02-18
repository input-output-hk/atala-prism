package io.iohk.cvp.cmanager.grpc

import io.iohk.cvp.cmanager.models.Issuer
import io.iohk.cvp.grpc.{GrpcAuthenticationHeader, GrpcAuthenticationHeaderParser}

package object services {
  private[grpc] def getIssuerId(): Issuer.Id = {
    GrpcAuthenticationHeaderParser.current() match {
      case Some(GrpcAuthenticationHeader.Legacy(userId)) => Issuer.Id(userId.uuid)
      case _ => throw new RuntimeException("userId header missing, legacy authentication expected")
    }
  }
}
