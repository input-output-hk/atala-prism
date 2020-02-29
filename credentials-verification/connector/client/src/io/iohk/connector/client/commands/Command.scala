package io.iohk.connector.client.commands

import java.security.{PrivateKey => JPrivateKey}

import com.google.protobuf.ByteString
import io.iohk.connector.client.Config
import io.iohk.cvp.connector.protos.ConnectorServiceGrpc
import io.iohk.cvp.crypto.ECSignature
import io.iohk.cvp.node_ops._

trait Command {
  def run(api: ConnectorServiceGrpc.ConnectorServiceBlockingStub, config: Config): Unit
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
