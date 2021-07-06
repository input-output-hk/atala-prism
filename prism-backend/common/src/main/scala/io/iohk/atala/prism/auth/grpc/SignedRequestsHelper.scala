package io.iohk.atala.prism.auth.grpc

import io.iohk.atala.prism.auth.model.RequestNonce

object SignedRequestsHelper {
  def merge(requestNonce: RequestNonce, request: Array[Byte]): Vector[Byte] = {
    requestNonce.bytes ++ request
  }
}
