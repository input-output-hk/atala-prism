package io.iohk.atala.prism.connector.client.commands

import com.google.protobuf.ByteString
import io.iohk.atala.crypto.{EC, ECPrivateKey}
import io.iohk.atala.prism.connector.client.Config
import io.iohk.atala.prism.protos.connector_api.ConnectorServiceGrpc
import io.iohk.atala.prism.protos.node_models

trait Command {
  def run(api: ConnectorServiceGrpc.ConnectorServiceBlockingStub, config: Config): Unit
}

object Command {

  def signOperation(
      operation: node_models.AtalaOperation,
      keyId: String,
      key: ECPrivateKey
  ): node_models.SignedAtalaOperation = {
    node_models.SignedAtalaOperation(
      signedWith = keyId,
      operation = Some(operation),
      signature = ByteString.copyFrom(EC.sign(operation.toByteArray, key).data)
    )
  }
}
