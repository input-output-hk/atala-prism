package io.iohk.atala.prism.connector

import cats.syntax.either._
import io.iohk.atala.prism.auth.errors.{AuthError, UnexpectedError}
import io.iohk.atala.prism.auth
import io.iohk.atala.prism.auth.SignedRequestsAuthenticatorBase
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class ConnectorAuthenticator(
    participantsRepository: ParticipantsRepository[IOWithTraceIdContext],
    requestNoncesRepository: RequestNoncesRepository[IOWithTraceIdContext],
    nodeClient: node_api.NodeServiceGrpc.NodeService,
    grpcAuthenticationHeaderParser: GrpcAuthenticationHeaderParser
) extends SignedRequestsAuthenticatorBase[ParticipantId](
      nodeClient,
      grpcAuthenticationHeaderParser
    ) {

  override def burnNonce(
      id: ParticipantId,
      requestNonce: auth.model.RequestNonce
  )(implicit ec: ExecutionContext): FutureEither[AuthError, Unit] =
    requestNoncesRepository
      .burn(id, requestNonce)
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
      .map(_.asRight)
      .toFutureEither

  override def burnNonce(
      did: DID,
      requestNonce: RequestNonce
  )(implicit
      ec: ExecutionContext
  ): FutureEither[AuthError, Unit] =
    requestNoncesRepository
      .burn(did, requestNonce)
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
      .map(_.asRight)
      .toFutureEither

  override def findByPublicKey(
      publicKey: ECPublicKey
  )(implicit ec: ExecutionContext): FutureEither[AuthError, ParticipantId] =
    participantsRepository
      .findBy(publicKey)
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
      .toFutureEither
      .mapLeft(_.unify)
      .map(_.id)
      .mapLeft(e => UnexpectedError(e.toStatus))

  override def findByDid(
      did: DID
  )(implicit ec: ExecutionContext): FutureEither[AuthError, ParticipantId] =
    participantsRepository
      .findBy(did)
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
      .toFutureEither
      .mapLeft(_.unify)
      .map(_.id)
      .mapLeft(e => UnexpectedError(e.toStatus))
}
