package io.iohk.atala.prism.auth

import io.grpc.Context
import io.iohk.atala.prism.crypto.{EC, ECPublicKey, ECSignature}
import io.iohk.atala.prism.auth.grpc.{GrpcAuthenticationHeader, GrpcAuthenticationHeaderParser, SignedRequestsHelper}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import io.iohk.atala.prism.auth.errors.{
  AuthError,
  AuthErrorSupport,
  CanonicalSuffixMatchStateError,
  InvalidAtalaOperationError,
  NoCreateDidOperationError,
  SignatureVerificationError,
  UnknownPublicKeyId
}
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.identity.DID.DIDFormat
import io.iohk.atala.prism.protos.node_models.CreateDIDOperation
import io.iohk.atala.prism.protos.{node_api, node_models}
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

trait Authenticator[Id] {
  def logger: Logger

  def authenticated[Request <: GeneratedMessage, Response <: GeneratedMessage](
      methodName: String,
      request: Request
  )(f: Id => Future[Response])(implicit ec: ExecutionContext): Future[Response]

  def public[Request <: GeneratedMessage, Response <: GeneratedMessage](
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
    * Finds a user associated with the given public key
    */
  def findByPublicKey(publicKey: ECPublicKey)(implicit ec: ExecutionContext): FutureEither[AuthError, Id]

  /**
    * Finds a user associated with the given DID
    */
  def findByDid(did: DID)(implicit ec: ExecutionContext): FutureEither[AuthError, Id]

  private def withLogging[Request <: GeneratedMessage, Response <: GeneratedMessage](
      methodName: String,
      request: Request,
      id: Id
  )(run: => Future[Response])(implicit ec: ExecutionContext): Future[Response] =
    run.andThen {
      case Success(response) =>
        logger.info(
          s"$methodName, id = $id, request = ${request.toProtoString}, response = ${response.toProtoString}"
        )
      case Failure(ex) =>
        logger.error(
          s"$methodName, id = $id, request = ${request.toProtoString}",
          ex
        )
    }

  private def withLogging[Request <: GeneratedMessage, Response <: GeneratedMessage](
      methodName: String,
      request: Request
  )(
      run: => Future[Response]
  )(implicit ec: ExecutionContext): Future[Response] =
    run.andThen {
      case Success(response) =>
        logger.info(
          s"$methodName, request = ${request.toProtoString}, response = ${response.toProtoString}"
        )
      case Failure(ex) =>
        logger.error(
          s"$methodName, request = ${request.toProtoString}",
          ex
        )
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
    val payload = SignedRequestsHelper.merge(requestNonce, request).toArray
    val verificationResultF = Future {
      Either
        .cond[AuthError, Unit](
          EC.verify(payload, publicKey, signature),
          (),
          SignatureVerificationError()
        )
    }
    for {
      _ <- verificationResultF.toFutureEither
      _ <- burnNonce(id, requestNonce)
    } yield id
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

  private def findPublicKey(didData: node_models.DIDData, keyId: String)(implicit
      ec: ExecutionContext
  ): FutureEither[AuthError, ECPublicKey] = {
    Future {
      // TODO: Validate keyUsage and revocation
      // we haven't defined which keys can sign requests, and the model doesn't specify when a key is revoked
      val publicKeyOpt = didData.publicKeys
        .find(_.id == keyId)
        .flatMap(_.keyData.ecKeyData)
        .map { data =>
          // TODO: Validate curve, right now we support a single curve
          EC.toPublicKey(x = data.x.toByteArray, y = data.y.toByteArray)
        }
      publicKeyOpt.toRight(UnknownPublicKeyId())
    }.toFutureEither
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
      publicKey <- findPublicKey(didDocument, authenticationHeader.keyId)

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
    authenticationHeader.did.getFormat match {
      case longFormDid: DIDFormat.LongForm =>
        longFormDid.validate match {
          case Left(DIDFormat.CanonicalSuffixMatchStateError) =>
            Future.successful(Left(CanonicalSuffixMatchStateError)).toFutureEither
          case Left(DIDFormat.InvalidAtalaOperationError) =>
            Future.successful(Left(InvalidAtalaOperationError)).toFutureEither
          case Right(validatedLongForm) =>
            validatedLongForm.initialState.operation.createDid match {
              case Some(CreateDIDOperation(Some(didData), _)) =>
                for {
                  publicKey <- findPublicKey(didData, authenticationHeader.keyId)
                  id <- findByDid(authenticationHeader.did)
                  _ <- verifyRequestSignature(
                    id = id,
                    publicKey = publicKey,
                    signature = authenticationHeader.signature,
                    request = request,
                    requestNonce = authenticationHeader.requestNonce
                  )
                } yield id
              case _ =>
                Future.successful(Left(NoCreateDidOperationError)).toFutureEither
            }
        }
      case _ => throw new IllegalStateException("Unreachable state")
    }
  }

  override def authenticated[Request <: GeneratedMessage, Response <: GeneratedMessage](
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
          logger.error(s"$methodName - missing userId, request = ${request.toProtoString}")
          Future.failed(throw new RuntimeException("Missing userId"))
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

  override def public[Request <: GeneratedMessage, Response <: GeneratedMessage](
      methodName: String,
      request: Request
  )(f: => Future[Response])(implicit ec: ExecutionContext): Future[Response] =
    withLogging(methodName, request)(f)
}
