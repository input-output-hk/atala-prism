package io.iohk.atala.prism.connector.client.commands

import com.google.protobuf.ByteString
import io.circe.syntax._
import io.circe.{Json, Printer}
import io.iohk.atala.prism.connector.client.Config
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.protos.connector_api.{AddConnectionFromTokenRequest, ConnectorServiceGrpc}
import io.iohk.atala.prism.protos.connector_models

case object AcceptConnection extends Command {
  override def run(api: ConnectorServiceGrpc.ConnectorServiceBlockingStub, config: Config): Unit = {
    val connectionToken: String = config.connectionToken.get
    val pubKey = EC.toPublicKey(x = BigInt(config.pubKeyX.get), y = BigInt(config.pubKeyY.get))
    val response = api.addConnectionFromToken(
      AddConnectionFromTokenRequest()
        .withToken(connectionToken)
        .withHolderEncodedPublicKey(connector_models.EncodedPublicKey(ByteString.copyFrom(pubKey.getEncoded)))
    )
    val ret = Json.obj(
      "connectionId" -> response.getConnection.connectionId.asJson,
      "token" -> response.getConnection.token.asJson
    )

    println(ret.printWith(new Printer(false, "  ")))
  }
}
