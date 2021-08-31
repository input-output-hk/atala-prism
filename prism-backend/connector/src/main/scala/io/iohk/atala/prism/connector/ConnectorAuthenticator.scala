package io.iohk.atala.prism.connector

import cats.effect.IO
import cats.syntax.either._
import io.iohk.atala.prism.auth.errors.{AuthError, UnexpectedError}
import io.iohk.atala.prism.auth
import io.iohk.atala.prism.auth.SignedRequestsAuthenticatorBase
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.identity.PrismDid
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class ConnectorAuthenticator(
    participantsRepository: ParticipantsRepository[IO],
    requestNoncesRepository: RequestNoncesRepository[IO],
    nodeClient: node_api.NodeServiceGrpc.NodeService,
    grpcAuthenticationHeaderParser: GrpcAuthenticationHeaderParser
) extends SignedRequestsAuthenticatorBase[ParticipantId](nodeClient, grpcAuthenticationHeaderParser) {

  override def burnNonce(
      id: ParticipantId,
      requestNonce: auth.model.RequestNonce
  )(implicit ec: ExecutionContext): FutureEither[AuthError, Unit] =
    requestNoncesRepository.burn(id, requestNonce).unsafeToFuture().map(_.asRight).toFutureEither

  override def burnNonce(
      did: PrismDid,
      requestNonce: RequestNonce
  )(implicit
      ec: ExecutionContext
  ): FutureEither[AuthError, Unit] =
    requestNoncesRepository.burn(did, requestNonce).unsafeToFuture().map(_.asRight).toFutureEither

  override def findByPublicKey(
      publicKey: ECPublicKey
  )(implicit ec: ExecutionContext): FutureEither[AuthError, ParticipantId] =
    participantsRepository
      .findBy(publicKey)
      .unsafeToFuture()
      .toFutureEither
      .map(_.id)
      .mapLeft(e => UnexpectedError(e.toStatus))

  override def findByDid(did: PrismDid)(implicit ec: ExecutionContext): FutureEither[AuthError, ParticipantId] =
    participantsRepository
      .findBy(did)
      .unsafeToFuture()
      .toFutureEither
      .map(_.id)
      .mapLeft(e => UnexpectedError(e.toStatus))
}
