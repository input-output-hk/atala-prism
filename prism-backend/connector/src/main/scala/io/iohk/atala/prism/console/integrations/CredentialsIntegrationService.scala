package io.iohk.atala.prism.console.integrations

import io.iohk.atala.prism.connector.errors
import io.iohk.atala.prism.console.models.{Institution, RevokePublishedCredential}
import io.iohk.atala.prism.console.repositories.CredentialsRepository
import io.iohk.atala.prism.models.TransactionId
import io.iohk.atala.prism.protos.{common_models, node_api}
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
  ): FutureEither[errors.ConnectorError, common_models.TransactionInfo] = {
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
      transactionId = {
        nodeResponse.transactionInfo
          .map(_.transactionId)
          .flatMap(TransactionId.from)
          .getOrElse(throw new RuntimeException("The node wasn't able to generate a transaction"))
      }
      transactionInfo = {
        nodeResponse.transactionInfo
          .getOrElse(throw new RuntimeException("We could not generate a transaction"))
      }

      // Update the database
      _ <- {
        credentialsRepository
          .storeRevocationData(institutionId, request.credentialId, transactionId)
      }
    } yield transactionInfo
  }
}
