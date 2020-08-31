package io.iohk.atala.prism.connector.client.commands

import io.circe.syntax._
import io.circe.{Json, Printer}
import io.iohk.atala.prism.connector.client.Config
import io.iohk.prism.protos.connector_api.{AddConnectionFromTokenRequest, ConnectorServiceGrpc}
import io.iohk.prism.protos.connector_models.ConnectorPublicKey

case object AcceptConnection extends Command {
  override def run(api: ConnectorServiceGrpc.ConnectorServiceBlockingStub, config: Config): Unit = {
    val connectionToken: String = config.connectionToken.get
    val pubKeyX = config.pubKeyX.get
    val pubKeyY = config.pubKeyY.get
    val response = api.addConnectionFromToken(
      AddConnectionFromTokenRequest(
        token = connectionToken,
        holderPublicKey = Some(ConnectorPublicKey(pubKeyX, pubKeyY))
      )
    )
    val ret = Json.obj(
      "connectionId" -> response.getConnection.connectionId.asJson,
      "token" -> response.getConnection.token.asJson
    )

    println(ret.printWith(new Printer(false, "  ")))
  }
}
