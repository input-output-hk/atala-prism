package io.iohk.atala.prism.connector

import io.grpc.Context
import io.iohk.atala.prism.crypto.{EC, ECPublicKey, ECSignature}
import io.iohk.atala.prism.grpc.{GrpcAuthenticationHeader, GrpcAuthenticationHeaderParser, SignedRequestsHelper}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import io.iohk.atala.prism.connector.errors.{ConnectorError, ErrorSupport, SignatureVerificationError}
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.protos.node_api
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

trait Authenticator {

  def logger: Logger

  def authenticated[Request <: GeneratedMessage, Response <: GeneratedMessage](
      methodName: String,
      request: Request
  )(
      f: ParticipantId => Future[Response]
  )(implicit ec: ExecutionContext): Future[Response]

  def public[Request <: GeneratedMessage, Response <: GeneratedMessage](methodName: String, request: Request)(
      f: => Future[Response]
  )(implicit ec: ExecutionContext): Future[Response]
}

trait AuthenticatorWithGrpcHeaderParser extends Authenticator {
  def grpcAuthenticationHeaderParser: GrpcAuthenticationHeaderParser
}

class SignedRequestsAuthenticator(
    participantsRepository: ParticipantsRepository,
    requestNoncesRepository: RequestNoncesRepository,
    nodeClient: node_api.NodeServiceGrpc.NodeService,
    override val grpcAuthenticationHeaderParser: GrpcAuthenticationHeaderParser
) extends AuthenticatorWithGrpcHeaderParser
    with ErrorSupport {

  override val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private def withLogging[Request <: GeneratedMessage, Response <: GeneratedMessage](
      methodName: String,
      request: Request,
      participantId: ParticipantId
  )(
      run: => Future[Response]
  )(implicit
      ec: ExecutionContext
  ): Future[Response] =
    run.andThen {
      case Success(response) =>
        logger.info(
          s"$methodName, userId = $participantId, request = ${request.toProtoString}, response = ${response.toProtoString}"
        )
      case Failure(ex) =>
        logger.error(
          s"$methodName, userId = $participantId, request = ${request.toProtoString}",
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
      participantId: ParticipantId,
      publicKey: ECPublicKey,
      request: Array[Byte],
      requestNonce: model.RequestNonce,
      signature: ECSignature
  )(implicit ec: ExecutionContext): FutureEither[SignatureVerificationError, ParticipantId] = {
    val payload = SignedRequestsHelper.merge(requestNonce, request).toArray
    val verificationResultF = Future {
      Either
        .cond(
          EC.verify(payload, publicKey, signature),
          participantId,
          SignatureVerificationError()
        )
    }
    for {
      _ <- verificationResultF.toFutureEither
      _ <- requestNoncesRepository.burn(participantId, requestNonce)
    } yield participantId
  }

  private def authenticate(request: Array[Byte], authenticationHeader: GrpcAuthenticationHeader)(implicit
      executionContext: ExecutionContext
  ): FutureEither[ConnectorError, ParticipantId] = {
    authenticationHeader match {
      case h: GrpcAuthenticationHeader.PublicKeyBased => authenticate(request, h)
      case h: GrpcAuthenticationHeader.DIDBased => authenticate(request, h)
    }
  }

  private def authenticate(request: Array[Byte], authenticationHeader: GrpcAuthenticationHeader.PublicKeyBased)(implicit
      ec: ExecutionContext
  ): FutureEither[ConnectorError, ParticipantId] = {

    for {
      // first we verify that we know the DID to avoid performing costly calls if we don't know it
      participantId <- participantsRepository.findBy(authenticationHeader.publicKey).map(_.id)
      signature = authenticationHeader.signature
      publicKey = authenticationHeader.publicKey
      _ <- verifyRequestSignature(
        participantId = participantId,
        publicKey = publicKey,
        signature = signature,
        request = request,
        requestNonce = authenticationHeader.requestNonce
      )
    } yield participantId
  }

  private def authenticate(request: Array[Byte], authenticationHeader: GrpcAuthenticationHeader.DIDBased)(implicit
      ec: ExecutionContext
  ): FutureEither[ConnectorError, ParticipantId] = {
    for {
      // first we verify that we know the DID to avoid performing costly calls if we don't know it
      participantId <- participantsRepository.findBy(authenticationHeader.did).map(_.id)

      didDocumentResponse <-
        nodeClient
          .getDidDocument(node_api.GetDidDocumentRequest(authenticationHeader.did))
          .map(Right(_))
          .toFutureEither

      didDocument = didDocumentResponse.document.getOrElse(throw new RuntimeException("Unknown DID"))
      // TODO: Validate keyUsage and revocation
      // we haven't defined which keys can sign requests, and the model doesn't specify when a key is revoked
      publicKey =
        didDocument.publicKeys
          .find(_.id == authenticationHeader.keyId)
          .flatMap(_.keyData.ecKeyData)
          .map { data =>
            // TODO: Validate curve, right now we support a single curve
            EC.toPublicKey(x = data.x.toByteArray, y = data.y.toByteArray)
          }
          .getOrElse(throw new RuntimeException("Unknown public key id"))

      // Verify the actual signature
      _ <- verifyRequestSignature(
        participantId = participantId,
        publicKey = publicKey,
        signature = authenticationHeader.signature,
        request = request,
        requestNonce = authenticationHeader.requestNonce
      )
    } yield participantId
  }

  override def authenticated[Request <: GeneratedMessage, Response <: GeneratedMessage](
      methodName: String,
      request: Request
  )(
      f: ParticipantId => Future[Response]
  )(implicit ec: ExecutionContext): Future[Response] = {
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

  override def public[Request <: GeneratedMessage, Response <: GeneratedMessage](methodName: String, request: Request)(
      f: => Future[Response]
  )(implicit ec: ExecutionContext): Future[Response] = {
    withLogging(methodName, request)(f)
  }
}
