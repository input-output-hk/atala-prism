package io.iohk.atala.prism.auth

import io.grpc.Context
import io.iohk.atala.prism.auth.errors.{AuthError, AuthErrorSupport, SignatureVerificationError}
import io.iohk.atala.prism.auth.grpc.{GrpcAuthenticationHeader, GrpcAuthenticationHeaderParser, SignedRequestsHelper}
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.auth.utils.DIDUtils
import io.iohk.atala.prism.crypto.{EC, ECPublicKey, ECSignature}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

trait Authenticator[Id] {
  def logger: Logger

  def whitelistedDid[Request <: GeneratedMessage, Response](
      whitelist: Set[DID],
      methodName: String,
      request: Request
  )(f: DID => Future[Response])(implicit ec: ExecutionContext): Future[Response]

  def authenticated[Request <: GeneratedMessage, Response](
      methodName: String,
      request: Request
  )(f: Id => Future[Response])(implicit ec: ExecutionContext): Future[Response]

  def public[Request <: GeneratedMessage, Response](
      methodName: String,
      request: Request
  )(f: => Future[Response])(implicit ec: ExecutionContext): Future[Response]
}

trait AuthenticatorWithGrpcHeaderParser[Id] extends Authenticator[Id] {
  def grpcAuthenticationHeaderParser: GrpcAuthenticationHeaderParser
}

abstract class SignedRequestsAuthenticatorBase[Id](
    nodeClient: node_api.NodeServiceGrpc.NodeService,
    override val grpcAuthenticationHeaderParser: GrpcAuthenticationHeaderParser
) extends AuthenticatorWithGrpcHeaderParser[Id]
    with AuthErrorSupport {

  override val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /**
    * Burns given nonce for user id, so that the request can not be cloned by a malicious agent
    */
  def burnNonce(id: Id, requestNonce: RequestNonce)(implicit ec: ExecutionContext): FutureEither[AuthError, Unit]

  /**
    * Burns given nonce for DID, so that the request can not be cloned by a malicious agent
    */
  def burnNonce(did: DID, requestNonce: RequestNonce)(implicit ec: ExecutionContext): FutureEither[AuthError, Unit]

  /**
    * Finds a user associated with the given public key
    */
  def findByPublicKey(publicKey: ECPublicKey)(implicit ec: ExecutionContext): FutureEither[AuthError, Id]

  /**
    * Finds a user associated with the given DID
    */
  def findByDid(did: DID)(implicit ec: ExecutionContext): FutureEither[AuthError, Id]

  private def withLogging[Request <: GeneratedMessage, Response](
      methodName: String,
      request: Request,
      id: Id
  )(run: => Future[Response])(implicit ec: ExecutionContext): Future[Response] =
    run.andThen {
      case Success(response: GeneratedMessage) =>
        logger.info(
          s"$methodName, id = $id, request = ${request.toProtoString}, response = ${response.toProtoString}"
        )
      case Success(response) =>
        logger.info(
          s"$methodName, id = $id, request = ${request.toProtoString}, response = $response"
        )
      case Failure(ex) =>
        logger.error(
          s"$methodName, id = $id, request = ${request.toProtoString}",
          ex
        )
    }

  private def withLogging[Request <: GeneratedMessage, Response](
      methodName: String,
      request: Request
  )(
      run: => Future[Response]
  )(implicit ec: ExecutionContext): Future[Response] =
    run.andThen {
      case Success(response: GeneratedMessage) =>
        logger.info(
          s"$methodName, request = ${request.toProtoString}, response = ${response.toProtoString}"
        )
      case Success(response) =>
        logger.info(
          s"$methodName, request = ${request.toProtoString}, response = $response"
        )
      case Failure(ex) =>
        logger.error(
          s"$methodName, request = ${request.toProtoString}",
          ex
        )
    }

  private def verifyRequestSignature(
      publicKey: ECPublicKey,
      request: Array[Byte],
      requestNonce: model.RequestNonce,
      signature: ECSignature
  )(implicit ec: ExecutionContext): FutureEither[AuthError, Unit] = {
    val payload = SignedRequestsHelper.merge(requestNonce, request).toArray
    Future {
      Either
        .cond[AuthError, Unit](
          EC.verify(payload, publicKey, signature),
          (),
          SignatureVerificationError()
        )
    }.toFutureEither
  }

  /**
    * A request must be signed by prepending the nonce, let's say requestNonce|request
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
  )(implicit ec: ExecutionContext): FutureEither[AuthError, Id] = {
    for {
      _ <- verifyRequestSignature(publicKey, request, requestNonce, signature)
      _ <- burnNonce(id, requestNonce)
    } yield id
  }

  private def verifyRequestSignature(
      did: DID,
      publicKey: ECPublicKey,
      request: Array[Byte],
      requestNonce: model.RequestNonce,
      signature: ECSignature
  )(implicit ec: ExecutionContext): FutureEither[AuthError, DID] = {
    for {
      _ <- verifyRequestSignature(publicKey, request, requestNonce, signature)
      _ <- burnNonce(did, requestNonce)
    } yield did
  }

  private def authenticate(request: Array[Byte], authenticationHeader: GrpcAuthenticationHeader)(implicit
      executionContext: ExecutionContext
  ): FutureEither[AuthError, Id] = {
    authenticationHeader match {
      case h: GrpcAuthenticationHeader.PublicKeyBased => authenticate(request, h)
      case h: GrpcAuthenticationHeader.PublishedDIDBased => authenticate(request, h)
      case h: GrpcAuthenticationHeader.UnpublishedDIDBased => authenticate(request, h)
    }
  }

  private def authenticate(request: Array[Byte], authenticationHeader: GrpcAuthenticationHeader.PublicKeyBased)(implicit
      ec: ExecutionContext
  ): FutureEither[AuthError, Id] = {

    for {
      // first we verify that we know the DID to avoid performing costly calls if we don't know it
      id <- findByPublicKey(authenticationHeader.publicKey)
      signature = authenticationHeader.signature
      publicKey = authenticationHeader.publicKey
      _ <- verifyRequestSignature(
        id = id,
        publicKey = publicKey,
        signature = signature,
        request = request,
        requestNonce = authenticationHeader.requestNonce
      )
    } yield id
  }

  private def authenticate(request: Array[Byte], authenticationHeader: GrpcAuthenticationHeader.PublishedDIDBased)(
      implicit ec: ExecutionContext
  ): FutureEither[AuthError, Id] = {
    for {
      // first we verify that we know the DID to avoid performing costly calls if we don't know it
      id <- findByDid(authenticationHeader.did)

      didDocumentResponse <-
        nodeClient
          .getDidDocument(node_api.GetDidDocumentRequest(authenticationHeader.did.value))
          .map(Right(_))
          .toFutureEither

      didDocument = didDocumentResponse.document.getOrElse(throw new RuntimeException("Unknown DID"))
      publicKey <- DIDUtils.findPublicKey(didDocument, authenticationHeader.keyId)

      // Verify the actual signature
      _ <- verifyRequestSignature(
        id = id,
        publicKey = publicKey,
        signature = authenticationHeader.signature,
        request = request,
        requestNonce = authenticationHeader.requestNonce
      )
    } yield id
  }

  private def authenticate(request: Array[Byte], authenticationHeader: GrpcAuthenticationHeader.UnpublishedDIDBased)(
      implicit ec: ExecutionContext
  ): FutureEither[AuthError, Id] = {
    DIDUtils.validateDid(authenticationHeader.did).flatMap { didData =>
      for {
        publicKey <- DIDUtils.findPublicKey(didData, authenticationHeader.keyId)
        id <- findByDid(authenticationHeader.did)
        _ <- verifyRequestSignature(
          id = id,
          publicKey = publicKey,
          signature = authenticationHeader.signature,
          request = request,
          requestNonce = authenticationHeader.requestNonce
        )
      } yield id
    }
  }

  override def authenticated[Request <: GeneratedMessage, Response](
      methodName: String,
      request: Request
  )(f: Id => Future[Response])(implicit ec: ExecutionContext): Future[Response] = {
    try {
      val ctx = Context.current()
      val result = grpcAuthenticationHeaderParser
        .parse(ctx)
        .map(authenticate(request.toByteArray, _))
        .map { value =>
          value.map(v => withLogging(methodName, request, v) { f(v) }).successMap(identity)
        }
        .getOrElse {
          logger.error(s"$methodName - unauthenticated, request = ${request.toProtoString}")
          Future.failed(throw new RuntimeException("Missing or bad authentication"))
        }
        .flatten

      result.onComplete {
        case Success(_) => () // This case is already handled above on the `withLogging` call
        case Failure(ex) => logger.error(s"$methodName FAILED request = ${request.toProtoString}", ex)
      }
      result
    } catch {
      case NonFatal(ex) =>
        logger.error(s"$methodName - FATAL ERROR, request = ${request.toProtoString}", ex)
        Future.failed(ex)
    }
  }

  override def public[Request <: GeneratedMessage, Response](
      methodName: String,
      request: Request
  )(f: => Future[Response])(implicit ec: ExecutionContext): Future[Response] =
    withLogging(methodName, request)(f)

  override def whitelistedDid[Request <: GeneratedMessage, Response](
      whitelist: Set[DID],
      methodName: String,
      request: Request
  )(f: DID => Future[Response])(implicit ec: ExecutionContext): Future[Response] = {
    val ctx = Context.current()
    val result = grpcAuthenticationHeaderParser.parse(ctx)
    result match {
      case Some(GrpcAuthenticationHeader.UnpublishedDIDBased(requestNonce, did, keyId, signature))
          if whitelist.contains(did) =>
        val result = DIDUtils
          .validateDid(did)
          .flatMap { didData =>
            for {
              publicKey <- DIDUtils.findPublicKey(didData, keyId)
              _ <- verifyRequestSignature(
                did = did,
                publicKey = publicKey,
                signature = signature,
                request = request.toByteArray,
                requestNonce = requestNonce
              )
            } yield did
          }
          .map(_ => f(did))
          .successMap(identity)
          .flatten

        result.onComplete {
          case Success(_) => () // This case is already handled above on the `withLogging` call
          case Failure(ex) => logger.error(s"$methodName FAILED request = ${request.toProtoString}", ex)
        }
        result
      case Some(GrpcAuthenticationHeader.UnpublishedDIDBased(_, _, _, _)) =>
        logger.error(s"$methodName - unauthenticated, request = ${request.toProtoString}")
        Future.failed(throw new RuntimeException("The supplied DID is not whitelisted"))
      case Some(_) =>
        logger.error(s"$methodName - unauthenticated, request = ${request.toProtoString}")
        Future.failed(throw new RuntimeException("Invalid authentication method: unpublished DID is required"))
      case None =>
        logger.error(s"$methodName - unauthenticated, request = ${request.toProtoString}")
        Future.failed(throw new RuntimeException("Missing or bad authentication"))
    }
  }
}
