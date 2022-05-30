package io.iohk.atala.prism.management.console.integrations

import cats.data.NonEmptyList
import cats.effect.{MonadCancelThrow, Resource}
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.comonad._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.{Comonad, Functor, Monad, MonadThrow}
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
import io.iohk.atala.prism.management.console.models.PaginatedQueryConstraints.ResultOrdering
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.{ContactsRepository, CredentialsRepository}
import io.iohk.atala.prism.models.ConnectionToken
import io.iohk.atala.prism.protos.connector_models.ContactConnection
import io.iohk.atala.prism.protos.console_models.ContactConnectionStatus
import io.iohk.atala.prism.protos.node_api.GetOperationInfoRequest
import io.iohk.atala.prism.protos.{connector_models, node_api}
import org.slf4j.{Logger, LoggerFactory}
import tofu.Execute
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.logging._

import scala.concurrent.Future

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
    contactsRepository: ContactsRepository[F],
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
      .flatMap(_.traverse(genericCredential => getGenericCredentialWithConnection(genericCredential, 1)))

  def getGenericCredentials(
      issuedBy: ParticipantId,
      query: GenericCredential.PaginatedQuery
  ): F[GetGenericCredentialsResult] = {

    val connectionStatusFilterOpt = query.filters.flatMap(x => x.contactConnectionStatus)
    connectionStatusFilterOpt match {
      case None =>
        getAndAppendConnectionStatus(
          credentialsRepository.getBy(issuedBy, query),
          credentialsRepository.countBy(issuedBy, query.filters)
        )
      case Some(connectionStatusFilter) =>
        for {
          allContacts <- contactsRepository.getBy(
            issuedBy,
            PaginatedQueryConstraints(ordering = ResultOrdering(Contact.SortBy.Name)),
            ignoreFilterLimit = true
          )
          allConnectionStatuses <- connector.getConnectionStatus(
            allContacts.map(_.details.connectionToken)
          )
          tokenToConnection =
            allConnectionStatuses
              .filter(_.connectionStatus == connectionStatusFilter)
              .map(c => ConnectionToken(c.connectionToken) -> c)
              .toMap
          filteredContacts = allContacts.filter(c => tokenToConnection.contains(c.details.connectionToken))
          credentials <-
            if (filteredContacts.isEmpty)
              List.empty.pure
            else
              credentialsRepository.getBy(issuedBy, query, NonEmptyList.fromFoldable(filteredContacts.map(_.contactId)))
          credentialsCount <-
            if (filteredContacts.isEmpty)
              0.pure
            else
              credentialsRepository.countBy(
                issuedBy,
                query.filters,
                NonEmptyList.fromFoldable(filteredContacts.map(_.contactId))
              )
        } yield GetGenericCredentialsResult(
          credentials
            .map { genericCredential =>
              GenericCredentialWithConnection(
                genericCredential,
                tokenToConnection(genericCredential.connectionToken)
              )
            },
          credentialsCount
        )
    }
  }

  def getContactCredentials(
      issuedBy: ParticipantId,
      contactId: Contact.Id
  ): F[GetGenericCredentialsResult] = {
    val genericCredentialSupplier = credentialsRepository
      .getBy(issuedBy, contactId)
    getAndAppendConnectionStatus(
      genericCredentialSupplier,
      genericCredentialSupplier.map(credentials => credentials.size)
    )
  }

  private def appendRevocationStatus(
      genericCredentials: List[GenericCredential]
  ): F[List[GenericCredential]] =
    genericCredentials.map { genericCredential =>
      genericCredential.revokedOnOperationId
        .map { operationId =>
          ex.deferFuture(nodeService.getOperationInfo(GetOperationInfoRequest(operationId.toProtoByteString)))
            .map(operationInfo =>
              genericCredential
                .copy(revokedOnOperationStatus = Some(OperationStatus.withName(operationInfo.operationStatus.name)))
            )
        }
        .getOrElse(ex.deferFuture(Future.successful(genericCredential)))
    }.sequence

  private def getAndAppendConnectionStatus(
      genericCredentialSupplier: => F[List[GenericCredential]],
      genericCredentialCount: => F[Int]
  ): F[GetGenericCredentialsResult] =
    for {
      genericCredentials <- genericCredentialSupplier
      genericCredentialCount <- genericCredentialCount
      genericCredentialsWithOperationStatus <- appendRevocationStatus(genericCredentials)
      connectionStatuses <- connector.getConnectionStatus(
        genericCredentialsWithOperationStatus.map(_.connectionToken)
      )
      tokenToConnection = connectionStatuses
        .map(c => ConnectionToken(c.connectionToken) -> c)
        .toMap
    } yield GetGenericCredentialsResult(
      genericCredentialsWithOperationStatus
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
        },
      genericCredentialCount
    )

  private def revokeCredsInNode(
      request: RevokePublishedCredential
  ): F[Either[ManagementConsoleError, NodeRevocationResponse]] = {
    ex.deferFuture(
      nodeService.scheduleOperations(
        node_api.ScheduleOperationsRequest(List(request.revokeCredentialsOperation))
      )
    ).map(
      ProtoConverter[
        node_api.ScheduleOperationsResponse,
        NodeRevocationResponse
      ].fromProto(_, None)
    ).map(_.toEither.left.map(wrapAsServerError))
  }

  private def getGenericCredentialWithConnection(
      in: GenericCredential,
      count: Int
  ): F[GenericCredentialWithConnection] =
    getAndAppendConnectionStatus(List(in).pure[F], count.pure[F]).map(_.data.head)

}

object CredentialsIntegrationService {

  def apply[F[_]: Execute: MonadCancelThrow, R[_]: Functor](
      credentialsRepository: CredentialsRepository[F],
      contactsRepository: ContactsRepository[F],
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
        contactsRepository,
        nodeService,
        connector
      )
    }

  def unsafe[F[_]: Execute: MonadCancelThrow, R[_]: Comonad](
      credentialsRepository: CredentialsRepository[F],
      contactsRepository: ContactsRepository[F],
      nodeService: node_api.NodeServiceGrpc.NodeService,
      connector: ConnectorClient[F],
      logs: Logs[R, F]
  ): CredentialsIntegrationService[F] =
    CredentialsIntegrationService(
      credentialsRepository,
      contactsRepository,
      nodeService,
      connector,
      logs
    ).extract

  def makeResource[F[_]: Execute: MonadCancelThrow, R[_]: Monad](
      credentialsRepository: CredentialsRepository[F],
      contactsRepository: ContactsRepository[F],
      nodeService: node_api.NodeServiceGrpc.NodeService,
      connector: ConnectorClient[F],
      logs: Logs[R, F]
  ): Resource[R, CredentialsIntegrationService[F]] =
    Resource.eval(
      CredentialsIntegrationService(
        credentialsRepository,
        contactsRepository,
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
      data: List[GenericCredentialWithConnection],
      totalCount: Int
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
