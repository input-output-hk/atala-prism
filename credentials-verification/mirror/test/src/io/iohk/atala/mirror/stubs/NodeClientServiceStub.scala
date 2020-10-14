package io.iohk.atala.mirror.stubs

import io.iohk.atala.mirror.services.NodeClientService
import io.iohk.prism.protos.node_api.GetCredentialStateResponse
import io.iohk.prism.protos.node_models.DIDData
import monix.eval.Task

class NodeClientServiceStub(
    didDocument: Map[String, DIDData] = Map.empty,
    getCredentialStateResponse: Map[String, GetCredentialStateResponse] = Map.empty
) extends NodeClientService {

  def getDidDocument(did: String): Task[Option[DIDData]] =
    Task.pure(didDocument.get(did))

  def getCredentialState(credentialId: String): Task[GetCredentialStateResponse] =
    Task.pure(getCredentialStateResponse.getOrElse(credentialId, GetCredentialStateResponse("", None, None)))
}
