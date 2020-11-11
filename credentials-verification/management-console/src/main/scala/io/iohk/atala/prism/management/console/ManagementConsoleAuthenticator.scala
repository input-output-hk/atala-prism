package io.iohk.atala.prism.management.console

import cats.syntax.either._
import io.iohk.atala.prism.auth.errors.{AuthError, UnexpectedError, UnsupportedAuthMethod}
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.auth.SignedRequestsAuthenticatorBase
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.crypto.ECPublicKey
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.management.console.models.ParticipantId
import io.iohk.atala.prism.management.console.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.{ExecutionContext, Future}

class ManagementConsoleAuthenticator(
    participantsRepository: ParticipantsRepository,
    requestNoncesRepository: RequestNoncesRepository,
    nodeClient: node_api.NodeServiceGrpc.NodeService,
    grpcAuthenticationHeaderParser: GrpcAuthenticationHeaderParser
) extends SignedRequestsAuthenticatorBase[ParticipantId](nodeClient, grpcAuthenticationHeaderParser) {

  override def burnNonce(id: ParticipantId, requestNonce: RequestNonce)(implicit
      ec: ExecutionContext
  ): FutureEither[AuthError, Unit] =
    requestNoncesRepository.burn(id, requestNonce)

  override def findByPublicKey(publicKey: ECPublicKey)(implicit
      ec: ExecutionContext
  ): FutureEither[AuthError, ParticipantId] =
    Future.successful(UnsupportedAuthMethod().asLeft[ParticipantId]).toFutureEither

  override def findByDid(did: DID)(implicit ec: ExecutionContext): FutureEither[AuthError, ParticipantId] =
    participantsRepository.findBy(did).map(_.id).mapLeft(e => UnexpectedError(e.toStatus))
}
