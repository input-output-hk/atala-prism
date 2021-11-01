package io.iohk.atala.prism.management.console.integrations

import cats.{Comonad, Functor, Monad}
import cats.effect.Resource
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.comonad._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.management.console.clients.ConnectorClient
import io.iohk.atala.prism.management.console.errors
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.grpc._
import io.iohk.atala.prism.management.console.integrations.CredentialsIntegrationService.{
  GenericCredentialWithConnection,
  GetGenericCredentialsResult
}
import io.iohk.atala.prism.management.console.models.GenericCredential.PaginatedQuery
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.CredentialsRepository
import io.iohk.atala.prism.models.ConnectionToken
import io.iohk.atala.prism.protos.connector_models.ContactConnection
import io.iohk.atala.prism.protos.console_models.ContactConnectionStatus
import io.iohk.atala.prism.protos.{connector_models, node_api}
import org.slf4j.{Logger, LoggerFactory}
import tofu.Execute
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.logging._
import cats.MonadThrow
import cats.effect.MonadCancelThrow

@derive(applyK)
trait CredentialsIntegrationService[F[_]] {
  def revokePublishedCredential(
      institutionId: ParticipantId,
      request: RevokePublishedCredential
  ): F[Either[ManagementConsoleError, AtalaOperationId]]

  def createGenericCredential(
      participantId: ParticipantId,
      createGenericCredential: CreateGenericCredential
  ): F[Either[errors.ManagementConsoleError, GenericCredentialWithConnection]]

  def getGenericCredentials(
      issuedBy: ParticipantId,
      query: GenericCredential.PaginatedQuery
  ): F[GetGenericCredentialsResult]

  def getContactCredentials(
      issuedBy: ParticipantId,
      contactId: Contact.Id
  ): F[GetGenericCredentialsResult]

}

private final class CredentialsIntegrationServiceImpl[F[_]: Monad](
    credentialsRepository: CredentialsRepository[F],
    nodeService: node_api.NodeServiceGrpc.NodeService,
    connector: ConnectorClient[F]
)(implicit ex: Execute[F])
    extends ManagementConsoleErrorSupport
    with CredentialsIntegrationService[F] {

  import CredentialsIntegrationService._

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def revokePublishedCredential(
      institutionId: ParticipantId,
      request: RevokePublishedCredential
  ): F[Either[ManagementConsoleError, AtalaOperationId]] =
    for {
      maybeNodeResponse <- revokeCredsInNode(request)
      maybeOperationId <- maybeNodeResponse.traverse(nodeResponse =>
        credentialsRepository
          .storeRevocationData(
            institutionId,
            request.credentialId,
            nodeResponse.operationId
          )
          .as(nodeResponse.operationId)
      )
    } yield maybeOperationId

  def createGenericCredential(
      participantId: ParticipantId,
      createGenericCredential: CreateGenericCredential
  ): F[Either[errors.ManagementConsoleError, GenericCredentialWithConnection]] =
    credentialsRepository
      .create(participantId, createGenericCredential)
      .flatMap(_.traverse(getGenericCredentialWithConnection))

  def getGenericCredentials(
      issuedBy: ParticipantId,
      query: GenericCredential.PaginatedQuery
  ): F[GetGenericCredentialsResult] =
    getAndAppendConnectionStatus(
      credentialsRepository
        .getBy(issuedBy, query)
    )

  def getContactCredentials(
      issuedBy: ParticipantId,
      contactId: Contact.Id
  ): F[GetGenericCredentialsResult] =
    getAndAppendConnectionStatus(
      credentialsRepository
        .getBy(issuedBy, contactId)
    )

  private def getAndAppendConnectionStatus(
      genericCredentialSupplier: => F[List[GenericCredential]]
  ): F[GetGenericCredentialsResult] =
    for {
      genericCredentials <- genericCredentialSupplier
      connectionStatuses <- connector.getConnectionStatus(
        genericCredentials.map(_.connectionToken)
      )
      tokenToConnection = connectionStatuses
        .map(c => ConnectionToken(c.connectionToken) -> c)
        .toMap
    } yield GetGenericCredentialsResult(
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

  private def revokeCredsInNode(
      request: RevokePublishedCredential
  ): F[Either[ManagementConsoleError, NodeRevocationResponse]] = {
    ex.deferFuture(
      nodeService.revokeCredentials(
        node_api
          .RevokeCredentialsRequest()
          .withSignedOperation(request.revokeCredentialsOperation)
      )
    ).map(
      ProtoConverter[
        node_api.RevokeCredentialsResponse,
        NodeRevocationResponse
      ].fromProto
    ).map(_.toEither.left.map(wrapAsServerError))
  }

  private def getGenericCredentialWithConnection(
      in: GenericCredential
  ): F[GenericCredentialWithConnection] =
    getAndAppendConnectionStatus(List(in).pure[F]).map(_.data.head)

}

object CredentialsIntegrationService {

  def apply[F[_]: Execute: MonadCancelThrow, R[_]: Functor](
      credentialsRepository: CredentialsRepository[F],
      nodeService: node_api.NodeServiceGrpc.NodeService,
      connector: ConnectorClient[F],
      logs: Logs[R, F]
  ): R[CredentialsIntegrationService[F]] =
    for {
      serviceLogs <- logs.service[CredentialsIntegrationService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, CredentialsIntegrationService[F]] = serviceLogs
      val logs: CredentialsIntegrationService[Mid[F, *]] =
        new CredentialsIntegrationServiceLogs[F]
      val mid = logs
      mid attach new CredentialsIntegrationServiceImpl[F](
        credentialsRepository,
        nodeService,
        connector
      )
    }

  def unsafe[F[_]: Execute: MonadCancelThrow, R[_]: Comonad](
      credentialsRepository: CredentialsRepository[F],
      nodeService: node_api.NodeServiceGrpc.NodeService,
      connector: ConnectorClient[F],
      logs: Logs[R, F]
  ): CredentialsIntegrationService[F] =
    CredentialsIntegrationService(
      credentialsRepository,
      nodeService,
      connector,
      logs
    ).extract

  def makeResource[F[_]: Execute: MonadCancelThrow, R[_]: Monad](
      credentialsRepository: CredentialsRepository[F],
      nodeService: node_api.NodeServiceGrpc.NodeService,
      connector: ConnectorClient[F],
      logs: Logs[R, F]
  ): Resource[R, CredentialsIntegrationService[F]] =
    Resource.eval(
      CredentialsIntegrationService(
        credentialsRepository,
        nodeService,
        connector,
        logs
      )
    )

  case class GenericCredentialWithConnection(
      genericCredential: GenericCredential,
      connection: connector_models.ContactConnection
  )
  case class GetGenericCredentialsResult(
      data: List[GenericCredentialWithConnection]
  )
}

private final class CredentialsIntegrationServiceLogs[F[_]: ServiceLogging[
  *[_],
  CredentialsIntegrationService[F]
]: MonadThrow]
    extends CredentialsIntegrationService[Mid[F, *]] {
  override def revokePublishedCredential(
      institutionId: ParticipantId,
      request: RevokePublishedCredential
  ): Mid[F, Either[ManagementConsoleError, AtalaOperationId]] =
    in =>
      info"revoking published credential $institutionId ${request.credentialId}" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while revoking published credential $er",
            _ => info"revoking published credential - successfully done"
          )
        )
        .onError(
          errorCause"encountered an error while revoking published credential" (
            _
          )
        )

  override def createGenericCredential(
      participantId: ParticipantId,
      createGenericCredential: CreateGenericCredential
  ): Mid[F, Either[ManagementConsoleError, GenericCredentialWithConnection]] =
    in =>
      info"creating generic credential $participantId ${createGenericCredential.contactId}" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while creating generic credential $er",
            _ => info"creating generic credential - successfully done"
          )
        )
        .onError(
          errorCause"encountered an error while creating generic credential" (_)
        )

  override def getGenericCredentials(
      issuedBy: ParticipantId,
      query: PaginatedQuery
  ): Mid[F, GetGenericCredentialsResult] =
    in =>
      info"getting generic credentials $issuedBy" *> in.flatTap(result =>
        info"getting generic credentials - successfully done, got ${result.data.size} entities"
      )

  override def getContactCredentials(
      issuedBy: ParticipantId,
      contactId: Contact.Id
  ): Mid[F, GetGenericCredentialsResult] =
    in =>
      info"getting contact credentials $issuedBy $contactId" *> in.flatTap(result =>
        info"getting contact credentials - successfully done, got ${result.data.size} entities"
      )
}
