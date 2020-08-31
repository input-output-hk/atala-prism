package io.iohk.atala.prism.grpc

import io.iohk.atala.prism.connector.model.RequestNonce

object SignedRequestsHelper {
  def merge(requestNonce: RequestNonce, request: Array[Byte]): Vector[Byte] = {
    requestNonce.bytes ++ request
  }
}
