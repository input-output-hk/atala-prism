package io.iohk.atala.prism.errors

import io.grpc.Status

import io.iohk.atala.prism.protos.credential_models.{AtalaMessage, AtalaErrorMessage}
import com.google.rpc.status.{Status => StatusProto}

trait PrismError {
  def toStatus: Status

  lazy val toAtalaMessage: AtalaMessage = {
    val status = toStatus
    val statusProto = StatusProto(
      code = status.getCode.value(),
      message = status.getDescription
    )
    val atalaErrorMessage = AtalaErrorMessage(status = Some(statusProto))
    AtalaMessage().withAtalaErrorMessage(atalaErrorMessage)
  }
}

trait PrismServerError extends PrismError {
  def cause: Throwable
}
