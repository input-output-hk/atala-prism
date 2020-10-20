package io.iohk.atala.mirror.stubs

import monix.eval.Task

import io.iohk.atala.mirror.services.NodeClientService
import io.iohk.atala.prism.protos.node_api.{GetCredentialStateResponse, IssueCredentialResponse}
import io.iohk.atala.prism.protos.node_models.DIDData

class NodeClientServiceStub(
    didDocument: Map[String, DIDData] = Map.empty,
    getCredentialStateResponse: Map[String, GetCredentialStateResponse] = Map.empty,
    issueCredentialResponse: Option[IssueCredentialResponse] = None
) extends NodeClientService {

  def getDidDocument(did: String): Task[Option[DIDData]] =
    Task.pure(didDocument.get(did))

  def getCredentialState(credentialId: String): Task[GetCredentialStateResponse] =
    Task.pure(getCredentialStateResponse.getOrElse(credentialId, GetCredentialStateResponse("", None, None)))

  def issueCredential(content: String): Task[IssueCredentialResponse] = {
    issueCredentialResponse match {
      case Some(response) => Task.pure(response)
      case None => Task.pure(IssueCredentialResponse())
    }
  }
}
