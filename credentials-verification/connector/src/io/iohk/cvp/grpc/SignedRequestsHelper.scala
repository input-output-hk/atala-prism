package io.iohk.cvp.grpc

import io.iohk.connector.model.RequestNonce

object SignedRequestsHelper {
  def merge(requestNonce: RequestNonce, request: Array[Byte]): Vector[Byte] = {
    requestNonce.bytes ++ request
  }
}
