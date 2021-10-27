package io.iohk.atala.prism.node.poc.batch

import io.iohk.atala.prism.crypto.MerkleInclusionProof
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.protos.node_api.{CreateDIDRequest, CreateDIDResponse}
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation

case class Connector(node: node_api.NodeServiceGrpc.NodeServiceBlockingStub) {
  def registerDID(
      signedAtalaOperation: SignedAtalaOperation
  ): CreateDIDResponse = {
    node
      .createDID(
        CreateDIDRequest(Some(signedAtalaOperation))
      )
  }

  // a tiny simulation of sending the credential
  private var credentialBatchChannel: List[(String, MerkleInclusionProof)] = Nil

  def sendCredentialAndProof(
      message: List[(String, MerkleInclusionProof)]
  ): Unit = {
    credentialBatchChannel = message
  }

  def receivedCredentialAndProof(): List[(String, MerkleInclusionProof)] = {
    credentialBatchChannel
  }
}
