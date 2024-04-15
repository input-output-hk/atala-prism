package io.iohk.atala.prism.node.errors

import com.google.rpc.status.{Status => StatusProto}
import io.grpc.Status
import io.iohk.atala.prism.protos.common_models.{AtalaErrorMessage, AtalaMessage}

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
