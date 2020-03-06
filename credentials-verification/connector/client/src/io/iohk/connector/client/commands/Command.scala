package io.iohk.connector.client.commands

import java.security.{PrivateKey => JPrivateKey}

import com.google.protobuf.ByteString
import io.iohk.connector.client.Config
import io.iohk.cvp.crypto.ECSignature
import io.iohk.prism.protos.connector_api.ConnectorServiceGrpc
import io.iohk.prism.protos.node_models

trait Command {
  def run(api: ConnectorServiceGrpc.ConnectorServiceBlockingStub, config: Config): Unit
}

object Command {

  def signOperation(
      operation: node_models.AtalaOperation,
      keyId: String,
      key: JPrivateKey
  ): node_models.SignedAtalaOperation = {
    node_models.SignedAtalaOperation(
      signedWith = keyId,
      operation = Some(operation),
      signature = ByteString.copyFrom(ECSignature.sign(key, operation.toByteArray).toArray)
    )
  }
}
