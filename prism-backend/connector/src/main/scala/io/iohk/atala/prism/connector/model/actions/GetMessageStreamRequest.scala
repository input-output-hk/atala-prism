package io.iohk.atala.prism.connector.model.actions

import io.iohk.atala.prism.connector.model.MessageId

case class GetMessageStreamRequest(lastSeenMessageId: Option[MessageId])
