package io.iohk.atala.prism.connector.client.commands

import java.util.Base64

import com.google.protobuf.ByteString
import io.iohk.atala.prism.connector.client.Config
import io.iohk.atala.prism.protos.connector_api.{ConnectorServiceGrpc, GetConnectionByTokenRequest, SendMessageRequest}

case object SendMessage extends Command {

  val base64 = Base64.getDecoder

  override def run(api: ConnectorServiceGrpc.ConnectorServiceBlockingStub, config: Config): Unit = {

    val connectionToken: String = config.connectionToken.get
    val message = ByteString.copyFrom(base64.decode(config.base64Message.get))

    val connection = api.getConnectionByToken(GetConnectionByTokenRequest(connectionToken)).getConnection

    api.sendMessage(SendMessageRequest(connection.connectionId, message))
    ()
  }
}
