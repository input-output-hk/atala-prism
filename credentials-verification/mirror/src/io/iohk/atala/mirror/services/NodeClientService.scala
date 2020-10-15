package io.iohk.atala.mirror.services

import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.protos.node_models.DIDData
import monix.eval.Task

trait NodeClientService {

  def getDidDocument(did: String): Task[Option[DIDData]]

  def getCredentialState(credentialId: String): Task[GetCredentialStateResponse]

}

class NodeClientServiceImpl(node: NodeServiceGrpc.NodeServiceStub) extends NodeClientService {

  def getDidDocument(did: String): Task[Option[DIDData]] =
    Task.fromFuture(node.getDidDocument(GetDidDocumentRequest(did))).map(_.document)

  def getCredentialState(credentialId: String): Task[GetCredentialStateResponse] =
    Task.fromFuture(node.getCredentialState(GetCredentialStateRequest(credentialId)))
}
