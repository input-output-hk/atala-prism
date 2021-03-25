package io.iohk.atala.prism.management.console.integrations

import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.management.console.grpc._
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.models.{NodeRevocationResponse, ParticipantId, RevokePublishedCredential}
import io.iohk.atala.prism.management.console.repositories.CredentialsRepository
import io.iohk.atala.prism.protos.{common_models, node_api}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

class CredentialsIntegrationService(
    credentialsRepository: CredentialsRepository,
    nodeService: node_api.NodeServiceGrpc.NodeService
)(implicit ec: ExecutionContext)
    extends ManagementConsoleErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def revokePublishedCredential(
      institutionId: ParticipantId,
      request: RevokePublishedCredential
  ): FutureEither[ManagementConsoleError, common_models.TransactionInfo] = {
    for {
      nodeResponse <-
        nodeService
          .revokeCredentials(
            node_api
              .RevokeCredentialsRequest()
              .withSignedOperation(request.revokeCredentialsOperation)
          )
          .map(ProtoConverter[node_api.RevokeCredentialsResponse, NodeRevocationResponse].fromProto)
          .map(_.toEither)
          .toFutureEither(ex => wrapAsServerError(ex))
      _ <- credentialsRepository.storeRevocationData(institutionId, request.credentialId, nodeResponse.transactionId)
    } yield nodeResponse.transactionInfo
  }
}
