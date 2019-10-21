package io.iohk.node

import io.iohk.node.geud_node._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import io.iohk.node.services.AtalaService

class NodeApi(atalaService: AtalaService)(implicit executionContext: ExecutionContext)
    extends NodeServiceGrpc.NodeService { self =>

  import io.iohk.node.atala_bitcoin._

  def publishDidDocument(request: PublishDidDocumentRequest): Future[PublishDidDocumentResponse] = {
    val txDefinition = AtalaTx.Definition.PublishDidDocument(request)
    val tx = AtalaTx(txDefinition)
    atalaService
      .publishAtalaTransaction(tx)
      .value
      .map {
        case Right(_) =>
          PublishDidDocumentResponse()
        case Left(left) =>
          // TODO: Decide on a correct representation of errors
          throw new Exception("Unexpected error trying to publish an atala transaction\n" + left)
      }

  }

  def getDidDocument(request: GetDidDocumentRequest): Future[GetDidDocumentResponse] = ???
  def getProofOfCredentialIssued(
      request: GetProofOfCredentialIssuedRequest
  ): Future[GetProofOfCredentialIssuedResponse] = ???
  def publishProofOfCredentialIssued(
      request: PublishProofOfCredentialIssuedRequest
  ): Future[PublishProofOfCredentialIssuedResponse] = ???
  def revokeProofOfCredentialIssued(
      request: RevokeProofOfCredentialIssuedRequest
  ): Future[RevokeProofOfCredentialIssuedResponse] = ???

}
