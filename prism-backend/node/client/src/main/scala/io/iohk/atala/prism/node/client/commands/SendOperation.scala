package io.iohk.atala.prism.node.client.commands

import java.io.{BufferedReader, InputStreamReader}
import java.util.Base64

import io.iohk.atala.prism.node.client.Config
import io.iohk.atala.prism.protos.{node_api, node_models}

case class SendOperation() extends Command {
  override def run(api: node_api.NodeServiceGrpc.NodeServiceBlockingStub, config: Config): Unit = {
    val input = new BufferedReader(new InputStreamReader(java.lang.System.in))
      .lines()
      .toArray
      .mkString("\n")
    val operationBytes = Base64.getDecoder.decode(input)
    val signedOperation = node_models.SignedAtalaOperation.parseFrom(operationBytes)

    println(s"Sending operation:\n${signedOperation.toProtoString}")

    val response = signedOperation.operation.map(_.operation) match {
      case Some(_: node_models.AtalaOperation.Operation.CreateDid) =>
        api.createDID(node_api.CreateDIDRequest().withSignedOperation(signedOperation))
      case Some(_: node_models.AtalaOperation.Operation.UpdateDid) =>
        api.updateDID(node_api.UpdateDIDRequest().withSignedOperation(signedOperation))
      case Some(_: node_models.AtalaOperation.Operation.IssueCredentialBatch) =>
        api.issueCredentialBatch(node_api.IssueCredentialBatchRequest().withSignedOperation(signedOperation))
      case Some(_: node_models.AtalaOperation.Operation.RevokeCredentials) =>
        api.revokeCredentials(node_api.RevokeCredentialsRequest().withSignedOperation(signedOperation))
      case Some(_: node_models.AtalaOperation.Operation.Empty.type) | None =>
        throw new IllegalArgumentException("Operation has operation field missing!")
    }

    println(s"Response:\n${response.toProtoString}")

  }
}
