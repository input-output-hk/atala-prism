package io.iohk.atala.prism.node.auth

import cats.data.EitherT
import cats.effect.Resource
import cats.syntax.comonad._
import cats.syntax.either._
import cats.syntax.traverse._
import cats.{Applicative, Comonad, Functor, Monad, MonadThrow}
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.node.auth.errors._
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.crypto.signature.ECSignature
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.node.auth.grpc.GrpcAuthenticationHeader
import io.iohk.atala.prism.node.auth.utils.DIDUtils
import io.iohk.atala.prism.protos.node_api
import scalapb.GeneratedMessage
import tofu.Execute
import tofu.higherKind.Mid
import tofu.logging.{Loggable, Logs, ServiceLogging}
import tofu.syntax.monadic._

import scala.util.Try
import scala.util.control.NonFatal

@derive(applyK)
trait WhitelistedAuthenticatorF[F[_]] {

  def whitelistedDid[Request <: GeneratedMessage, Response](
      whitelist: Set[DID],
      methodName: String,
      request: Request,
      header: Option[GrpcAuthenticationHeader]
  ): F[Either[AuthError, DID]]

  def public[Request <: GeneratedMessage, Response](
      methodName: String,
      request: Request
  ): F[Unit]
}

@derive(applyK)
trait AuthenticatorF[Id, F[_]] extends WhitelistedAuthenticatorF[F] {

  def authenticated[Request <: GeneratedMessage, Response](
      methodName: String,
      request: Request,
      header: Option[GrpcAuthenticationHeader]
  ): F[Either[AuthError, Id]]
}

private[auth] class WhitelistedAuthenticatorFImpl[F[_]: Monad](burnAuth: WhitelistedAuthHelper[F])
    extends WhitelistedAuthenticatorF[F] {

  override def public[Request <: GeneratedMessage, Response](
      methodName: String,
      request: Request
  ): F[Unit] = Monad[F].unit

  override def whitelistedDid[Request <: GeneratedMessage, Response](
      whitelist: Set[DID],
      methodName: String,
      request: Request,
      grpcHeader: Option[GrpcAuthenticationHeader]
  ): F[Either[AuthError, DID]] = {
    grpcHeader match {
      case Some(
            GrpcAuthenticationHeader.UnpublishedDIDBased(
              requestNonce,
              did,
              keyId,
              signature
            )
          ) if whitelist.contains(did) =>
        {
          for {
            didData <- EitherT.fromEither[F](
              DIDUtils
                .validateDidEith(did)
            )
            publicKey <- EitherT.fromEither[F](DIDUtils.findPublicKeyEith(didData, keyId))
            _ <- EitherT(
              verifyRequestSignature(
                did = did,
                publicKey = publicKey,
                signature = signature,
                request = request.toByteArray,
                requestNonce = requestNonce
              )
            )
          } yield did
        }.value

      case Some(GrpcAuthenticationHeader.UnpublishedDIDBased(_, _, _, _)) =>
        Either
          .left[AuthError, DID](
            InvalidRequest(
              s"$methodName - unauthenticated, request = ${request.toProtoString}, cause the supplied DID is not whitelisted"
            )
          )
          .pure[F]

      case Some(_) =>
        Either
          .left[AuthError, DID](
            InvalidRequest(
              s"$methodName - unauthenticated, request = ${request.toProtoString}, cause invalid authentication method: unpublished DID is required"
            )
          )
          .pure[F]

      case None =>
        Either
          .left[AuthError, DID](
            InvalidRequest(
              s"$methodName - unauthenticated, request = ${request.toProtoString}, cause missing authentication"
            )
          )
          .pure[F]

    }
  }

  private def verifyRequestSignature(
      did: DID,
      publicKey: ECPublicKey,
      request: Array[Byte],
      requestNonce: model.RequestNonce,
      signature: ECSignature
  ): F[Either[AuthError, DID]] = {
    for {
      _ <- EitherT(verifyRequestSignature(publicKey, request, requestNonce, signature))
      _ <- EitherT.liftF[F, AuthError, Unit](burnAuth.burnNonce(did, requestNonce))
    } yield did
  }.value

  protected def verifyRequestSignature(
      publicKey: ECPublicKey,
      request: Array[Byte],
      requestNonce: model.RequestNonce,
      signature: ECSignature
  ): F[Either[AuthError, Unit]] = {
    val payload = requestNonce.mergeWith(request).toArray
    val isVerified = Try(EC.verifyBytes(payload, publicKey, signature)).getOrElse(false)
    Applicative[F].pure(
      Either
        .cond[AuthError, Unit](
          isVerified,
          (),
          SignatureVerificationError()
        )
    )
  }
}

private[auth] class AuthenticatorFImpl[Id, F[_]: Monad: Execute](
    nodeClient: node_api.NodeServiceGrpc.NodeService,
    burnAuth: AuthHelper[Id, F]
) extends WhitelistedAuthenticatorFImpl[F](burnAuth)
    with AuthenticatorF[Id, F] {

  /** A request must be signed by prepending the nonce, let's say requestNonce|request
    *
    * The signature is valid if the signature matches and the nonce hasn't been seen before.
    *
    * After the request is validated successfully, the request nonce is burn to prevent replay attacks.
    */
  private def verifyRequestSignature(
      id: Id,
      publicKey: ECPublicKey,
      request: Array[Byte],
      requestNonce: model.RequestNonce,
      signature: ECSignature
  ): F[Either[AuthError, Id]] = {
    for {
      _ <- EitherT(verifyRequestSignature(publicKey, request, requestNonce, signature))
      _ <- EitherT.liftF[F, AuthError, Unit](burnAuth.burnNonce(id, requestNonce))
    } yield id
  }.value

  private def authenticate(
      request: Array[Byte],
      authenticationHeader: GrpcAuthenticationHeader
  ): F[Either[AuthError, Id]] = {
    authenticationHeader match {
      case h: GrpcAuthenticationHeader.PublicKeyBased =>
        authenticate(request, h)
      case h: GrpcAuthenticationHeader.PublishedDIDBased =>
        authenticate(request, h)
      case h: GrpcAuthenticationHeader.UnpublishedDIDBased =>
        authenticate(request, h)
    }
  }

  private def authenticate(
      request: Array[Byte],
      authenticationHeader: GrpcAuthenticationHeader.PublicKeyBased
  ): F[Either[AuthError, Id]] = {

    for {
      // first we verify that we know the DID to avoid performing costly calls if we don't know it
      id <- EitherT(burnAuth.findByPublicKey(authenticationHeader.publicKey))
      signature = authenticationHeader.signature
      publicKey = authenticationHeader.publicKey
      _ <- EitherT(
        verifyRequestSignature(
          id = id,
          publicKey = publicKey,
          signature = signature,
          request = request,
          requestNonce = authenticationHeader.requestNonce
        )
      )
    } yield id
  }.value

  private def authenticate(
      request: Array[Byte],
      authenticationHeader: GrpcAuthenticationHeader.PublishedDIDBased
  ): F[Either[AuthError, Id]] = {
    for {
      // first we verify that we know the DID to avoid performing costly calls if we don't know it
      id <- EitherT(burnAuth.findByDid(authenticationHeader.did))

      didDocumentResponse <-
        EitherT.liftF(
          Execute[F].deferFuture(
            nodeClient
              .getDidDocument(
                node_api.GetDidDocumentRequest(authenticationHeader.did.getValue)
              )
          )
        )

      didDocument <- EitherT.fromOption(didDocumentResponse.document, InvalidRequest("Unknown DID"))
      publicKey <- EitherT.fromEither[F](
        DIDUtils.findPublicKeyEith(
          didDocument,
          authenticationHeader.keyId
        )
      )

      // Verify the actual signature
      _ <- EitherT(
        verifyRequestSignature(
          id = id,
          publicKey = publicKey,
          signature = authenticationHeader.signature,
          request = request,
          requestNonce = authenticationHeader.requestNonce
        )
      )
    } yield id
  }.value

  private def authenticate(
      request: Array[Byte],
      authenticationHeader: GrpcAuthenticationHeader.UnpublishedDIDBased
  ): F[Either[AuthError, Id]] = {
    DIDUtils.validateDidEith(authenticationHeader.did).flatTraverse { didData =>
      {
        for {
          publicKey <- EitherT.fromEither[F](DIDUtils.findPublicKeyEith(didData, authenticationHeader.keyId))
          id <- EitherT(burnAuth.findByDid(authenticationHeader.did))
          _ <- EitherT(
            verifyRequestSignature(
              id = id,
              publicKey = publicKey,
              signature = authenticationHeader.signature,
              request = request,
              requestNonce = authenticationHeader.requestNonce
            )
          )
        } yield id
      }.value
    }
  }

  override def authenticated[Request <: GeneratedMessage, Response](
      methodName: String,
      request: Request,
      grpcHeader: Option[GrpcAuthenticationHeader]
  ): F[Either[AuthError, Id]] = {
    try {
      grpcHeader match {
        case Some(header) => authenticate(request.toByteArray, header)
        case None =>
          Either
            .left[AuthError, Id](
              InvalidRequest(
                s"$methodName - unauthenticated, request = ${request.toProtoString}, cause missing authentication"
              )
            )
            .pure[F]
      }

    } catch {
      case NonFatal(ex) =>
        Either
          .left[AuthError, Id](
            InvalidRequest(s"$methodName - NonFatal ERROR ${ex.getMessage}, request = ${request.toProtoString}")
          )
          .pure[F]

    }
  }
}

object AuthenticatorF {
  def make[Id: Loggable, F[_]: MonadThrow: Execute, I[_]: Functor](
      nodeClient: node_api.NodeServiceGrpc.NodeService,
      burnAuth: AuthHelper[Id, F],
      logs: Logs[I, F]
  ): I[AuthenticatorF[Id, F]] = {
    for {
      serviceLogs <- logs.service[AuthenticatorF[Id, F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, AuthenticatorF[Id, F]] =
        serviceLogs
      val logs: AuthenticatorF[Id, Mid[F, *]] = new AuthenticatorFLogs[Id, F]
      val mid = logs
      mid attach new AuthenticatorFImpl[Id, F](
        nodeClient,
        burnAuth
      )
    }
  }

  def unsafe[Id: Loggable, F[_]: MonadThrow: Execute, I[_]: Comonad](
      nodeClient: node_api.NodeServiceGrpc.NodeService,
      burnAuth: AuthHelper[Id, F],
      logs: Logs[I, F]
  ): AuthenticatorF[Id, F] = AuthenticatorF.make(nodeClient, burnAuth, logs).extract

  def resource[Id: Loggable, F[_]: MonadThrow: Execute, I[_]: Functor](
      nodeClient: node_api.NodeServiceGrpc.NodeService,
      burnAuth: AuthHelper[Id, F],
      logs: Logs[I, F]
  ): Resource[I, AuthenticatorF[Id, F]] = Resource.eval(AuthenticatorF.make(nodeClient, burnAuth, logs))
}

object WhitelistedAuthenticatorF {
  def make[F[_]: MonadThrow: Execute, I[_]: Functor](
      burnAuth: WhitelistedAuthHelper[F],
      logs: Logs[I, F]
  ): I[WhitelistedAuthenticatorF[F]] = {
    for {
      serviceLogs <- logs.service[WhitelistedAuthenticatorF[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, WhitelistedAuthenticatorF[F]] =
        serviceLogs
      val logs: WhitelistedAuthenticatorF[Mid[F, *]] = new WhitelistedAuthenticatorFLogs[F]
      val mid = logs
      mid attach new WhitelistedAuthenticatorFImpl[F](
        burnAuth
      )
    }
  }

  def unsafe[F[_]: MonadThrow: Execute, I[_]: Comonad](
      burnAuth: WhitelistedAuthHelper[F],
      logs: Logs[I, F]
  ): WhitelistedAuthenticatorF[F] = WhitelistedAuthenticatorF.make(burnAuth, logs).extract

  def resource[F[_]: MonadThrow: Execute, I[_]: Functor](
      burnAuth: WhitelistedAuthHelper[F],
      logs: Logs[I, F]
  ): Resource[I, WhitelistedAuthenticatorF[F]] = Resource.eval(WhitelistedAuthenticatorF.make(burnAuth, logs))
}
