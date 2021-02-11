package io.iohk.atala.prism.stubs

import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot
import io.iohk.atala.prism.crypto.SHA256Digest
import monix.eval.Task
import io.iohk.atala.prism.services.NodeClientService
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.node_api.{
  GetBatchStateResponse,
  GetCredentialRevocationTimeResponse,
  IssueCredentialBatchResponse
}
import io.iohk.atala.prism.protos.node_models.DIDData

class NodeClientServiceStub(
    didDocument: Map[DID, DIDData] = Map.empty,
    getBatchStateResponse: Map[CredentialBatchId, GetBatchStateResponse] = Map.empty,
    issueCredentialBatchResponse: Option[IssueCredentialBatchResponse] = None,
    credentialRevocationTimeResponse: Map[(CredentialBatchId, SHA256Digest), GetCredentialRevocationTimeResponse] =
      Map.empty
) extends NodeClientService {

  def getDidDocument(did: DID): Task[Option[DIDData]] =
    Task.pure(didDocument.get(did))

  def getBatchState(credentialBatchId: CredentialBatchId): Task[Option[GetBatchStateResponse]] =
    Task.pure(getBatchStateResponse.get(credentialBatchId))

  def issueCredentialBatch(merkleRoot: MerkleRoot): Task[IssueCredentialBatchResponse] = {
    issueCredentialBatchResponse match {
      case Some(response) => Task.pure(response)
      case None => Task.pure(IssueCredentialBatchResponse())
    }
  }

  def getCredentialRevocationTime(
      credentialBatchId: CredentialBatchId,
      credentialHash: SHA256Digest
  ): Task[GetCredentialRevocationTimeResponse] =
    Task.pure {
      credentialRevocationTimeResponse.getOrElse(
        (credentialBatchId, credentialHash),
        GetCredentialRevocationTimeResponse()
      )
    }

}
