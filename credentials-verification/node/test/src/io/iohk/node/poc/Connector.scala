package io.iohk.node.poc

import io.iohk.prism.protos.node_api
import io.iohk.prism.protos.node_api.CreateDIDRequest
import io.iohk.prism.protos.node_models.SignedAtalaOperation

case class Connector(node: node_api.NodeServiceGrpc.NodeServiceBlockingStub) {
  def registerDID(signedAtalaOperation: SignedAtalaOperation): String = {
    node
      .createDID(
        CreateDIDRequest(Some(signedAtalaOperation))
      )
      .id
  }

  // This may not live in the connector, but eventually we will need a place to validate
  // which are the trusted issuer DIDs
  def isTrustedIssuer(did: String): Boolean = true

  // a tiny simulation of sending the credential
  private var credentialChannel: String = ""

  def sendCredential(c: String): Unit = {
    credentialChannel = c
  }

  def receivedCredential(): String = {
    credentialChannel
  }
}
