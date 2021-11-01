package io.iohk.atala.prism.management.console

import cats.effect.unsafe.IORuntime
import cats.syntax.either._
import io.iohk.atala.prism.auth.errors.{AuthError, UnexpectedError, UnsupportedAuthMethod}
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.auth.SignedRequestsAuthenticatorBase
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.management.console.models.ParticipantId
import io.iohk.atala.prism.management.console.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.{ExecutionContext, Future}

class ManagementConsoleAuthenticator(
    participantsRepository: ParticipantsRepository[IOWithTraceIdContext],
    requestNoncesRepository: RequestNoncesRepository[IOWithTraceIdContext],
    nodeClient: node_api.NodeServiceGrpc.NodeService,
    grpcAuthenticationHeaderParser: GrpcAuthenticationHeaderParser
)(implicit runtime: IORuntime) extends SignedRequestsAuthenticatorBase[ParticipantId](
      nodeClient,
      grpcAuthenticationHeaderParser
    ) {

  override def burnNonce(
      id: ParticipantId,
      requestNonce: RequestNonce,
      traceId: TraceId
  )(implicit
      ec: ExecutionContext
  ): FutureEither[AuthError, Unit] =
    requestNoncesRepository
      .burn(id, requestNonce)
      .run(traceId)
      .unsafeToFuture()
      .map(_.asRight)
      .toFutureEither

  override def burnNonce(
      did: DID,
      requestNonce: RequestNonce,
      traceId: TraceId
  )(implicit
      ec: ExecutionContext
  ): FutureEither[AuthError, Unit] =
    throw new NotImplementedError()

  override def findByPublicKey(publicKey: ECPublicKey, traceId: TraceId)(implicit
      ec: ExecutionContext
  ): FutureEither[AuthError, ParticipantId] =
    Future
      .successful(UnsupportedAuthMethod().asLeft[ParticipantId])
      .toFutureEither

  override def findByDid(did: DID, traceId: TraceId)(implicit
      ec: ExecutionContext
  ): FutureEither[AuthError, ParticipantId] =
    participantsRepository
      .findBy(did)
      .run(traceId)
      .unsafeToFuture()
      .toFutureEither
      .map(_.id)
      .mapLeft(e => UnexpectedError(e.toStatus))
}
