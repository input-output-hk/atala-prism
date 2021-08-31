package io.iohk.atala.prism.vault

import io.iohk.atala.prism.auth.errors.{AuthError, UnsupportedAuthMethod}
import io.iohk.atala.prism.auth.SignedRequestsAuthenticatorBase
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.identity.PrismDid
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import io.iohk.atala.prism.vault.repositories.RequestNoncesRepository

import scala.concurrent.{ExecutionContext, Future}

class VaultAuthenticator(
    requestNoncesRepository: RequestNoncesRepository[IOWithTraceIdContext],
    nodeClient: node_api.NodeServiceGrpc.NodeService,
    grpcAuthenticationHeaderParser: GrpcAuthenticationHeaderParser
) extends SignedRequestsAuthenticatorBase[PrismDid](nodeClient, grpcAuthenticationHeaderParser) {

  override def burnNonce(did: PrismDid, requestNonce: RequestNonce)(implicit
      ec: ExecutionContext
  ): FutureEither[AuthError, Unit] =
    requestNoncesRepository.burn(did, requestNonce).run(TraceId.generateYOLO).unsafeToFuture().lift

  override def findByPublicKey(publicKey: ECPublicKey)(implicit
      ec: ExecutionContext
  ): FutureEither[AuthError, PrismDid] =
    Future(Left(UnsupportedAuthMethod())).toFutureEither

  override def findByDid(did: PrismDid)(implicit ec: ExecutionContext): FutureEither[AuthError, PrismDid] =
    Future.successful(Right(did)).toFutureEither
}
