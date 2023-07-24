package io.iohk.atala.prism.node.poc.batch

import io.iohk.atala.prism.crypto.MerkleInclusionProof
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.protos.node_api.ScheduleOperationsRequest
import io.iohk.atala.prism.protos.node_models.{OperationOutput, SignedAtalaOperation}

case class Connector(node: node_api.NodeServiceGrpc.NodeServiceBlockingStub) {
  def registerDID(
      signedAtalaOperation: SignedAtalaOperation
  ): OperationOutput = {
    node
      .scheduleOperations(
        ScheduleOperationsRequest(List(signedAtalaOperation))
      )
      .outputs
      .head
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
