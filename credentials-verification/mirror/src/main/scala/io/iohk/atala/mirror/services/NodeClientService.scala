package io.iohk.atala.mirror.services

import monix.eval.Task
import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.{EC, SHA256Digest}
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.protos.node_models._
import io.iohk.atala.mirror.NodeUtils
import io.iohk.atala.prism.services.BaseGrpcClientService.DidBasedAuthConfig
import io.iohk.atala.prism.identity.DID

trait NodeClientService {

  def getDidDocument(did: DID): Task[Option[DIDData]]

  def getCredentialState(credentialId: String): Task[GetCredentialStateResponse]

  def issueCredential(content: String): Task[IssueCredentialResponse]

}

class NodeClientServiceImpl(node: NodeServiceGrpc.NodeServiceStub, authConfig: DidBasedAuthConfig)
    extends NodeClientService {

  def getDidDocument(did: DID): Task[Option[DIDData]] =
    Task.fromFuture(node.getDidDocument(GetDidDocumentRequest(did.value))).map(_.document)

  def getCredentialState(credentialId: String): Task[GetCredentialStateResponse] =
    Task.fromFuture(node.getCredentialState(GetCredentialStateRequest(credentialId)))

  def issueCredential(content: String): Task[IssueCredentialResponse] = {
    val operation =
      NodeUtils.issueCredentialOperation(SHA256Digest.compute(content.getBytes), authConfig.did)

    val signedAtalaOperation =
      SignedAtalaOperation(
        signedWith = authConfig.didKeyId,
        operation = Some(operation),
        signature = ByteString.copyFrom(EC.sign(operation.toByteArray, authConfig.didKeyPair.privateKey).data)
      )

    Task.fromFuture(node.issueCredential(IssueCredentialRequest().withSignedOperation(signedAtalaOperation)))
  }

}
