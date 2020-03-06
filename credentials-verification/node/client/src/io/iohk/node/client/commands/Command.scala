package io.iohk.node.client.commands

import java.security.{PrivateKey => JPrivateKey}

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.ECSignature
import io.iohk.node.client.Config
import io.iohk.prism.protos.{node_api, node_models}

trait Command {
  def run(api: node_api.NodeServiceGrpc.NodeServiceBlockingStub, config: Config): Unit
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
