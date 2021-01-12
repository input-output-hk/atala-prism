package io.iohk.atala.prism.node.client.commands

import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.{EC, ECPrivateKey}
import io.iohk.atala.prism.node.client.Config
import io.iohk.atala.prism.protos.{node_api, node_models}

trait Command {
  def run(api: node_api.NodeServiceGrpc.NodeServiceBlockingStub, config: Config): Unit
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
