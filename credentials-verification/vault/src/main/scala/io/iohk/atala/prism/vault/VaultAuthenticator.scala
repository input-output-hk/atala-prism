package io.iohk.atala.prism.vault

import io.iohk.atala.prism.auth.errors.{AuthError, UnsupportedAuthMethod}
import io.iohk.atala.prism.auth.SignedRequestsAuthenticatorBase
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.crypto.ECPublicKey
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import io.iohk.atala.prism.vault.repositories.RequestNoncesRepository

import scala.concurrent.{ExecutionContext, Future}

class VaultAuthenticator(
    requestNoncesRepository: RequestNoncesRepository,
    nodeClient: node_api.NodeServiceGrpc.NodeService,
    grpcAuthenticationHeaderParser: GrpcAuthenticationHeaderParser
) extends SignedRequestsAuthenticatorBase[DID](nodeClient, grpcAuthenticationHeaderParser) {

  override def burnNonce(did: DID, requestNonce: RequestNonce)(implicit
      ec: ExecutionContext
  ): FutureEither[AuthError, Unit] =
    requestNoncesRepository.burn(did, requestNonce)

  override def findByPublicKey(publicKey: ECPublicKey)(implicit
      ec: ExecutionContext
  ): FutureEither[AuthError, DID] =
    Future(Left(UnsupportedAuthMethod())).toFutureEither

  override def findByDid(did: DID)(implicit ec: ExecutionContext): FutureEither[AuthError, DID] =
    Future.successful(Right(did)).toFutureEither
}
