package io.iohk.atala.prism.connector.client.commands

import io.circe.Printer
import io.circe.generic.auto._
import io.circe.syntax._
import io.iohk.atala.prism.connector.client.Config
import io.iohk.atala.prism.connector.model.{Connection, ConnectionId, TokenString}
import io.iohk.atala.prism.protos.connector_api.{ConnectorServiceGrpc, GetConnectionByTokenRequest}

case object GetConnection extends Command {
  override def run(api: ConnectorServiceGrpc.ConnectorServiceBlockingStub, config: Config): Unit = {
    val connectionToken: String = config.connectionToken.get
    val response = api.getConnectionByToken(GetConnectionByTokenRequest(token = connectionToken))
    val connection = Connection(
      connectionToken = new TokenString(response.getConnection.connectionToken),
      connectionId = ConnectionId.unsafeFrom(response.getConnection.connectionId)
    )
    println(connection.asJson.printWith(new Printer(false, "  ")))
  }
}
