package io.iohk.atala.prism.console.integrations

import io.iohk.atala.prism.connector.{AtalaOperationId, errors}
import io.iohk.atala.prism.console.models.{Institution, RevokePublishedCredential}
import io.iohk.atala.prism.console.repositories.CredentialsRepository
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class CredentialsIntegrationService(
    credentialsRepository: CredentialsRepository,
    nodeService: node_api.NodeServiceGrpc.NodeService
)(implicit ec: ExecutionContext) {

  def revokePublishedCredential(
      institutionId: Institution.Id,
      request: RevokePublishedCredential
  ): FutureEither[errors.ConnectorError, AtalaOperationId] = {
    for {
      // send the operation to the Node
      nodeResponse <- {
        nodeService
          .revokeCredentials(
            node_api
              .RevokeCredentialsRequest()
              .withSignedOperation(request.signedOperation)
          )
          .map(Right(_))
          .toFutureEither
      }

      operationId = AtalaOperationId.fromVectorUnsafe(nodeResponse.operationId.toByteArray.toVector)

      // Update the database
      _ <- {
        credentialsRepository
          .storeRevocationData(institutionId, request.credentialId, operationId)
      }
    } yield operationId
  }
}
