package io.iohk.node.client.commands

import java.io.{BufferedReader, InputStreamReader}
import java.util.Base64

import io.iohk.node.client.Config
import io.iohk.prism.protos.{node_api, node_models}

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
      case Some(_: node_models.AtalaOperation.Operation.IssueCredential) =>
        api.issueCredential(node_api.IssuerCredentialRequest().withSignedOperation(signedOperation))
      case Some(_: node_models.AtalaOperation.Operation.RevokeCredential) =>
        api.revokeCredential(node_api.RevokeCredentialRequest().withSignedOperation(signedOperation))
      case None =>
        throw new IllegalArgumentException("Operation has operation field missing!")
    }

    println(s"Response:\n${response.toProtoString}")

  }
}
