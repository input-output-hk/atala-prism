package io.iohk.atala.prism.connector.model.actions

import cats.data.NonEmptyList
import io.iohk.atala.prism.connector.model.{MessageId, TokenString}

case class SendMessagesRequest(
    messages: Option[NonEmptyList[SendMessagesRequest.MessageToSend]]
)

object SendMessagesRequest {
  case class MessageToSend(
      connectionToken: TokenString,
      message: Array[Byte],
      id: Option[MessageId]
  )
}
