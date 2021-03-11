package io.iohk.atala.prism.connector.client.commands

import java.time.Instant
import java.util.Base64

import io.circe.Printer
import io.circe.generic.auto._
import io.circe.syntax._
import io.iohk.atala.prism.connector.client.Config
import io.iohk.atala.prism.protos.connector_api.{
  ConnectorServiceGrpc,
  GetConnectionByTokenRequest,
  GetMessagesForConnectionRequest
}
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.utils.syntax._

import scala.annotation.nowarn

case object GetMessages extends Command {

  override def run(api: ConnectorServiceGrpc.ConnectorServiceBlockingStub, config: Config): Unit = {

    val connectionToken: String = config.connectionToken.get

    val response = api.getConnectionByToken(GetConnectionByTokenRequest(token = connectionToken))

    val messagesResponse =
      api.getMessagesForConnection(GetMessagesForConnectionRequest(connectionId = response.getConnection.connectionId))

    messagesResponse.messages.foreach(printMessage)
  }

  private def printMessage(m: ReceivedMessage): Unit = {
    println(EncodedMessage(m).asJson.printWith(GetMessages.printer))
  }

  val base64 = Base64.getEncoder

  val printer = new Printer(false, "  ")

  case class EncodedMessage(messageId: String, connectionId: String, receivedAt: Instant, content: String)

  object EncodedMessage {
    @nowarn("msg=value receivedDeprecated in class ReceivedMessage is deprecated")
    def apply(m: ReceivedMessage): EncodedMessage = {
      EncodedMessage(
        messageId = m.id,
        connectionId = m.connectionId,
        receivedAt = m.received.fold(Instant.ofEpochMilli(m.receivedDeprecated))(_.toInstant),
        content = base64.encodeToString(m.message.toByteArray)
      )
    }
  }
}
