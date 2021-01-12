package io.iohk.atala.prism.errors

import io.grpc.Status

trait PrismError {
  def toStatus: Status
}

trait PrismServerError extends PrismError {
  def cause: Throwable
}
