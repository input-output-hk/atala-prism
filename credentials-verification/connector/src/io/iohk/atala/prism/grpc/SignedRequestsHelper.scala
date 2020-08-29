package io.iohk.atala.prism.grpc

import io.iohk.connector.model.RequestNonce

object SignedRequestsHelper {
  def merge(requestNonce: RequestNonce, request: Array[Byte]): Vector[Byte] = {
    requestNonce.bytes ++ request
  }
}
