package io.iohk.atala.prism.node.poc.toyflow

import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.protos.node_api.CreateDIDRequest
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation

case class Connector(node: node_api.NodeServiceGrpc.NodeServiceBlockingStub) {
  def registerDID(signedAtalaOperation: SignedAtalaOperation): String = {
    node
      .createDID(
        CreateDIDRequest(Some(signedAtalaOperation))
      )
      .id
  }

  // a tiny simulation of sending the credential
  private var credentialChannel: String = ""

  def sendCredential(c: String): Unit = {
    credentialChannel = c
  }

  def receivedCredential(): String = {
    credentialChannel
  }
}
