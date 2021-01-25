package io.iohk.atala.prism.connector.grpc

import io.iohk.atala.prism.connector.model.ContactConnection
import io.iohk.atala.prism.console.grpc.ProtoCodecs.contactConnectionStatus2Proto
import io.iohk.atala.prism.protos.connector_models
import io.scalaland.chimney.Transformer

object ProtoCodecs {
  implicit val contactConnection2Proto: Transformer[ContactConnection, connector_models.ContactConnection] =
    contactConnection => {
      connector_models.ContactConnection(
        connectionId = contactConnection.connectionId.map(_.toString).getOrElse(""),
        connectionToken = contactConnection.contactToken.map(_.token).getOrElse(""),
        connectionStatus = contactConnectionStatus2Proto.transform(contactConnection.connectionStatus)
      )
    }
}
