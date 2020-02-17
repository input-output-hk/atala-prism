package io.iohk.node.client.commands

import java.security.{PrivateKey => JPrivateKey}

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.ECSignature
import io.iohk.node.client.Config
import io.iohk.node.geud_node.{NodeServiceGrpc, _}

trait Command {
  def run(api: NodeServiceGrpc.NodeServiceBlockingStub, config: Config): Unit
}

object Command {

  def signOperation(
      operation: AtalaOperation,
      keyId: String,
      key: JPrivateKey
  ): SignedAtalaOperation = {
    SignedAtalaOperation(
      signedWith = keyId,
      operation = Some(operation),
      signature = ByteString.copyFrom(ECSignature.sign(key, operation.toByteArray).toArray)
    )
  }
}
