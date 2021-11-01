package io.iohk.atala.prism.connector.services

import cats.effect.Resource
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.comonad._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{Applicative, Comonad, Functor}
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.connector.errors._
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.ConnectionsRepository
import io.iohk.atala.prism.connector.repositories.ConnectionsRepository._
import io.iohk.atala.prism.connector.services.ConnectionsService.GetConnectionCommunicationKeysError
import io.iohk.atala.prism.connector.services.logs.ConnectionsServiceLogs
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import org.slf4j.LoggerFactory
import shapeless.{:+:, CNil}
import tofu.Execute
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import cats.MonadThrow
import cats.effect.MonadCancelThrow

@derive(applyK)
trait ConnectionsService[F[_]] {
  def getConnectionByToken(token: TokenString): F[Option[Connection]]

  def getConnectionById(
      participantId: ParticipantId,
      id: ConnectionId
  ): F[Option[ConnectionInfo]]

  def generateTokens(
      userId: ParticipantId,
      tokensCount: Int
  ): F[List[TokenString]]

  def getTokenInfo(
      token: TokenString
  ): F[Either[GetTokenInfoError, ParticipantInfo]]

  def addConnectionFromToken(
      tokenString: TokenString,
      didOrPublicKey: Either[DID, ECPublicKey]
  ): F[Either[AddConnectionFromTokenError, ConnectionInfo]]

  def revokeConnection(
      participantId: ParticipantId,
      connectionId: ConnectionId
  ): F[Either[RevokeConnectionError, Unit]]

  def getConnectionsPaginated(
      userId: ParticipantId,
      limit: Int,
      lastSeenConnectionId: Option[ConnectionId]
  ): F[Either[GetConnectionsPaginatedError, List[ConnectionInfo]]]

  def getConnectionCommunicationKeys(
      connectionId: ConnectionId,
      userId: ParticipantId
  ): F[Either[GetConnectionCommunicationKeysError, Seq[(String, ECPublicKey)]]]

  def getConnectionsByConnectionTokens(
      connectionTokens: List[TokenString]
  ): F[List[ContactConnection]]
}

object ConnectionsService {
  type GetConnectionCommunicationKeysError = InternalConnectorError :+: CNil

  def apply[F[_]: MonadCancelThrow: Execute, R[_]: Functor](
      connectionsRepository: ConnectionsRepository[F],
      nodeService: NodeServiceGrpc.NodeService,
      logs: Logs[R, F]
  ): R[ConnectionsService[F]] =
    for {
      serviceLogs <- logs.service[ConnectionsService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, ConnectionsService[F]] =
        serviceLogs
      val logs: ConnectionsService[Mid[F, *]] = new ConnectionsServiceLogs[F]
      val mid = logs
      mid attach new ConnectionsServiceImpl[F](
        connectionsRepository,
        nodeService
      )
    }

  def resource[F[_]: MonadCancelThrow: Execute, R[_]: Applicative: Functor](
      connectionsRepository: ConnectionsRepository[F],
      nodeService: NodeServiceGrpc.NodeService,
      logs: Logs[R, F]
  ): Resource[R, ConnectionsService[F]] =
    Resource.eval(ConnectionsService(connectionsRepository, nodeService, logs))

  def unsafe[F[_]: MonadCancelThrow: Execute, R[_]: Comonad](
      connectionsRepository: ConnectionsRepository[F],
      nodeService: NodeServiceGrpc.NodeService,
      logs: Logs[R, F]
  ): ConnectionsService[F] =
    ConnectionsService(connectionsRepository, nodeService, logs).extract
}

private class ConnectionsServiceImpl[F[_]: MonadThrow](
    connectionsRepository: ConnectionsRepository[F],
    nodeService: NodeServiceGrpc.NodeService
)(implicit ex: Execute[F])
    extends ConnectionsService[F] {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getConnectionByToken(token: TokenString): F[Option[Connection]] =
    connectionsRepository.getConnectionByToken(token)

  override def getConnectionById(
      participantId: ParticipantId,
      id: ConnectionId
  ): F[Option[ConnectionInfo]] =
    connectionsRepository.getConnection(participantId, id)

  def generateTokens(
      userId: ParticipantId,
      tokensCount: Int
  ): F[List[TokenString]] =
    connectionsRepository
      .insertTokens(userId, List.fill(tokensCount)(TokenString.random()))

  def getTokenInfo(
      token: TokenString
  ): F[Either[GetTokenInfoError, ParticipantInfo]] =
    connectionsRepository.getTokenInfo(token)

  def addConnectionFromToken(
      tokenString: TokenString,
      didOrPublicKey: Either[DID, ECPublicKey]
  ): F[Either[AddConnectionFromTokenError, ConnectionInfo]] =
    connectionsRepository.addConnectionFromToken(tokenString, didOrPublicKey)

  def revokeConnection(
      participantId: ParticipantId,
      connectionId: ConnectionId
  ): F[Either[RevokeConnectionError, Unit]] =
    connectionsRepository
      .revokeConnection(participantId, connectionId)

  def getConnectionsPaginated(
      userId: ParticipantId,
      limit: Int,
      lastSeenConnectionId: Option[ConnectionId]
  ): F[Either[GetConnectionsPaginatedError, List[ConnectionInfo]]] =
    connectionsRepository
      .getConnectionsPaginated(userId, limit, lastSeenConnectionId)

  def getConnectionCommunicationKeys(
      connectionId: ConnectionId,
      userId: ParticipantId
  ): F[
    Either[GetConnectionCommunicationKeysError, Seq[(String, ECPublicKey)]]
  ] = {
    def getDidCommunicationKeys(
        did: DID
    ): F[
      Either[GetConnectionCommunicationKeysError, Seq[(String, ECPublicKey)]]
    ] = {
      val request = node_api.GetDidDocumentRequest(did = did.getValue)
      val result = for {
        response <- ex.deferFuture(nodeService.getDidDocument(request))
        allKeys = response.document.map(_.publicKeys).getOrElse(Seq.empty)
        validKeys = allKeys.filter(key => key.revokedOn.isEmpty)
        // TODO: select communication keys only, once we provision them and make frontend use them
      } yield validKeys.map { key =>
        val keyData = key.keyData.ecKeyData.getOrElse(
          throw new Exception("Node returned key without keyData")
        )
        assert(keyData.curve == ECConfig.getCURVE_NAME)
        (
          key.id,
          EC.toPublicKeyFromByteCoordinates(
            keyData.x.toByteArray,
            keyData.y.toByteArray
          )
        )
      }

      result
        .map[Either[GetConnectionCommunicationKeysError, Seq[
          (String, ECPublicKey)
        ]]](Right(_))
        .recover { case ex =>
          Left(co(InternalConnectorError(ex)))
        }
    }

    for {
      participantInfo <-
        connectionsRepository
          .getOtherSideInfo(connectionId, userId)
          .map(_.get)
      keys <- (participantInfo.did, participantInfo.publicKey) match {
        case (Some(did), keyOpt) =>
          if (keyOpt.isDefined) {
            logger.warn(
              s"Both DID and keys found for user ${userId}, using DID keys only"
            )
          }
          getDidCommunicationKeys(did)
        case (None, Some(key)) => Right(Seq(("", key))).pure[F]
        case (None, None) => Right(Seq.empty).pure[F]
      }
    } yield keys
  }

  def getConnectionsByConnectionTokens(
      connectionTokens: List[TokenString]
  ): F[List[ContactConnection]] =
    connectionsRepository
      .getConnectionsByConnectionTokens(connectionTokens)
}
