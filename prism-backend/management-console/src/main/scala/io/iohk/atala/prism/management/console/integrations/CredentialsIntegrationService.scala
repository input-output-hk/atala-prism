package io.iohk.atala.prism.management.console.integrations

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.management.console.clients.ConnectorClient
import io.iohk.atala.prism.management.console.errors
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.grpc._
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.CredentialsRepository
import io.iohk.atala.prism.models.ConnectionToken
import io.iohk.atala.prism.protos.connector_models.ContactConnection
import io.iohk.atala.prism.protos.console_models.ContactConnectionStatus
import io.iohk.atala.prism.protos.{connector_models, node_api}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.{FutureEitherFOps, FutureEitherOps}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class CredentialsIntegrationService(
    credentialsRepository: CredentialsRepository[IO],
    nodeService: node_api.NodeServiceGrpc.NodeService,
    connector: ConnectorClient
)(implicit ec: ExecutionContext)
    extends ManagementConsoleErrorSupport {

  import CredentialsIntegrationService._

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def revokePublishedCredential(
      institutionId: ParticipantId,
      request: RevokePublishedCredential
  ): FutureEither[ManagementConsoleError, AtalaOperationId] = {
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
      _ <-
        credentialsRepository
          .storeRevocationData(
            institutionId,
            request.credentialId,
            nodeResponse.operationId
          )
          .unsafeToFuture()
          .map(_.asRight)
          .toFutureEither
    } yield nodeResponse.operationId
  }

  def createGenericCredential(
      participantId: ParticipantId,
      createGenericCredential: CreateGenericCredential
  ): Future[Either[errors.ManagementConsoleError, GenericCredentialWithConnection]] = {
    getAndAppendConnectionStatus(
      credentialsRepository
        .create(participantId, createGenericCredential)
        .unsafeToFuture()
        .toFutureEither
        .map(credential => List(credential))
    ).map(_.map(result => result.data.head))
  }

  def getGenericCredentials(
      issuedBy: ParticipantId,
      query: GenericCredential.PaginatedQuery
  ): Future[Either[Nothing, GetGenericCredentialsResult]] =
    getAndAppendConnectionStatus(
      credentialsRepository.getBy(issuedBy, query).map(_.asRight).unsafeToFuture().toFutureEither
    )

  def getContactCredentials(
      issuedBy: ParticipantId,
      contactId: Contact.Id
  ): Future[Either[Nothing, GetGenericCredentialsResult]] =
    getAndAppendConnectionStatus(
      credentialsRepository.getBy(issuedBy, contactId).map(_.asRight).unsafeToFuture().toFutureEither
    )

  private def getAndAppendConnectionStatus[E](
      genericCredentialSupplier: => FutureEither[E, List[GenericCredential]]
  ): Future[Either[E, GetGenericCredentialsResult]] = {
    val result = for {
      genericCredentials <- genericCredentialSupplier
      connectionStatuses <- connector.getConnectionStatus(genericCredentials.map(_.connectionToken)).lift
      tokenToConnection = connectionStatuses.map(c => ConnectionToken(c.connectionToken) -> c).toMap
    } yield {
      GetGenericCredentialsResult(
        genericCredentials
          .map { genericCredential =>
            GenericCredentialWithConnection(
              genericCredential,
              tokenToConnection.getOrElse(
                genericCredential.connectionToken,
                ContactConnection(
                  connectionToken = genericCredential.connectionToken.token,
                  connectionStatus = ContactConnectionStatus.STATUS_CONNECTION_MISSING
                )
              )
            )
          }
      )
    }
    result.value
  }
}

object CredentialsIntegrationService {
  case class GenericCredentialWithConnection(
      genericCredential: GenericCredential,
      connection: connector_models.ContactConnection
  )
  case class GetGenericCredentialsResult(data: List[GenericCredentialWithConnection])
}
