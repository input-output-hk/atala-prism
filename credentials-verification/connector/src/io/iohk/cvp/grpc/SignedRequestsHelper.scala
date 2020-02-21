package io.iohk.cvp.grpc

object SignedRequestsHelper {
  def merge(requestNonce: Vector[Byte], request: Array[Byte]): Vector[Byte] = {
    requestNonce ++ request
  }
}
