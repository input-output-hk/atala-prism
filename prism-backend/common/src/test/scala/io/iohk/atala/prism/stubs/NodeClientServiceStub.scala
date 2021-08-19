package io.iohk.atala.prism.stubs

import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.crypto.MerkleRoot
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import monix.eval.Task
import io.iohk.atala.prism.services.NodeClientService
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.protos.node_api.{
  GetBatchStateResponse,
  GetCredentialRevocationTimeResponse,
  IssueCredentialBatchResponse
}
import io.iohk.atala.prism.protos.node_models.DIDData

class NodeClientServiceStub(
    didDocument: Map[DID, DIDData] = Map.empty,
    getBatchStateResponse: Map[CredentialBatchId, GetBatchStateResponse] = Map.empty,
    issueCredentialBatchResponse: Task[IssueCredentialBatchResponse] = Task.pure(IssueCredentialBatchResponse()),
    credentialRevocationTimeResponse: Map[(CredentialBatchId, SHA256Digest), GetCredentialRevocationTimeResponse] =
      Map.empty
) extends NodeClientService {

  def getDidDocument(did: DID): Task[Option[DIDData]] =
    Task.pure(didDocument.get(did))

  def getBatchState(credentialBatchId: CredentialBatchId): Task[Option[GetBatchStateResponse]] =
    Task.pure(getBatchStateResponse.get(credentialBatchId))

  def issueCredentialBatch(merkleRoot: MerkleRoot): Task[IssueCredentialBatchResponse] = issueCredentialBatchResponse

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
