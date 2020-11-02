package io.iohk.atala.prism.connector

import io.iohk.atala.prism.auth.errors.{AuthError, UnexpectedError}
import io.iohk.atala.prism.auth
import io.iohk.atala.prism.auth.SignedRequestsAuthenticatorBase
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.crypto.ECPublicKey
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.utils.FutureEither

import scala.concurrent.ExecutionContext

class ConnectorAuthenticator(
    participantsRepository: ParticipantsRepository,
    requestNoncesRepository: RequestNoncesRepository,
    nodeClient: node_api.NodeServiceGrpc.NodeService,
    grpcAuthenticationHeaderParser: GrpcAuthenticationHeaderParser
) extends SignedRequestsAuthenticatorBase[ParticipantId](nodeClient, grpcAuthenticationHeaderParser) {

  override def burnNonce(
      id: ParticipantId,
      requestNonce: auth.model.RequestNonce
  )(implicit ec: ExecutionContext): FutureEither[AuthError, Unit] =
    requestNoncesRepository.burn(id, requestNonce)

  override def findByPublicKey(
      publicKey: ECPublicKey
  )(implicit ec: ExecutionContext): FutureEither[AuthError, ParticipantId] =
    participantsRepository.findBy(publicKey).map(_.id).mapLeft(e => UnexpectedError(e.toStatus))

  override def findByDid(did: String)(implicit ec: ExecutionContext): FutureEither[AuthError, ParticipantId] =
    participantsRepository.findBy(did).map(_.id).mapLeft(e => UnexpectedError(e.toStatus))
}
