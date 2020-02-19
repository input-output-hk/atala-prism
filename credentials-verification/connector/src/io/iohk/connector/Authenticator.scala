package io.iohk.connector

import io.iohk.connector.errors.{ConnectorError, ErrorSupport, SignatureVerificationError}
import io.iohk.connector.repositories.ConnectionsRepository
import io.iohk.cvp.crypto.ECKeys.toPublicKey
import io.iohk.cvp.crypto.{ECKeys, ECSignature}
import io.iohk.cvp.grpc.{GrpcAuthenticationHeader, GrpcAuthenticationHeaderParser}
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither._
import io.iohk.nodenew.node_api
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait Authenticator {

  import Authenticator._

  def logger: Logger

  def authenticated[Request <: GeneratedMessage, Response <: GeneratedMessage](
      methodName: String,
      request: Request
  )(
      f: ParticipantId => Future[Response]
  )(implicit ec: ExecutionContext, headersParser: RequestHeadersParser): Future[Response]

  def public[Request <: GeneratedMessage, Response <: GeneratedMessage](methodName: String, request: Request)(
      f: => Future[Response]
  )(implicit ec: ExecutionContext): Future[Response]
}

object Authenticator {

  trait RequestHeadersParser {
    def parseAuthenticationHeader: Option[GrpcAuthenticationHeader]
  }

  implicit val gRPCHeadersParser: RequestHeadersParser = new RequestHeadersParser {
    override def parseAuthenticationHeader: Option[GrpcAuthenticationHeader] = {
      GrpcAuthenticationHeaderParser.current()
    }
  }
}

class SignedRequestsAuthenticator(
    connectionsRepository: ConnectionsRepository,
    nodeClient: node_api.NodeServiceGrpc.NodeService
) extends Authenticator
    with ErrorSupport {

  import Authenticator._

  override val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private def withLogging[Request <: GeneratedMessage, Response <: GeneratedMessage](
      methodName: String,
      request: Request,
      participantId: ParticipantId
  )(
      run: => Future[Response]
  )(
      implicit ec: ExecutionContext
  ): Future[Response] =
    run.andThen {
      case Success(response) =>
        logger.info(
          s"$methodName, userId = $participantId, request = ${request.toProtoString}, response = ${response.toProtoString}"
        )
      case Failure(ex) =>
        logger.error(
          s"$methodName, userId = $participantId, request = ${request.toProtoString}, $ex"
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
          s"$methodName, request = ${request.toProtoString}, $ex"
        )
    }

  private def authenticate(request: Array[Byte], authenticationHeader: GrpcAuthenticationHeader)(
      implicit executionContext: ExecutionContext
  ): FutureEither[ConnectorError, ParticipantId] = {
    authenticationHeader match {
      case GrpcAuthenticationHeader.Legacy(userId) => Future.successful(Right(userId)).toFutureEither
      case h: GrpcAuthenticationHeader.PublicKeyBased => authenticate(request, h)
      case h: GrpcAuthenticationHeader.DIDBased => authenticate(request, h)
    }
  }

  private def authenticate(request: Array[Byte], authenticationHeader: GrpcAuthenticationHeader.PublicKeyBased)(
      implicit ec: ExecutionContext
  ): FutureEither[ConnectorError, ParticipantId] = {

    for {
      signature <- Future { Right(authenticationHeader.signature) }.toFutureEither
      publicKey <- Future { Right(toPublicKey(authenticationHeader.publicKey)) }.toFutureEither
      _ <- Either
        .cond(
          ECSignature.verify(publicKey, request, signature),
          (),
          SignatureVerificationError()
        )
        .toFutureEither
      participantId <- connectionsRepository.getParticipantId(authenticationHeader.publicKey)
    } yield participantId
  }

  private def authenticate(request: Array[Byte], authenticationHeader: GrpcAuthenticationHeader.DIDBased)(
      implicit ec: ExecutionContext
  ): FutureEither[ConnectorError, ParticipantId] = {
    for {
      // first we verify that we know the DID to avoid performing costly calls if we don't know it
      participantId <- connectionsRepository.getParticipantId(authenticationHeader.did)

      didDocumentResponse <- nodeClient
        .getDidDocument(node_api.GetDidDocumentRequest(authenticationHeader.did))
        .map(Right(_))
        .toFutureEither

      didDocument = didDocumentResponse.document.getOrElse(throw new RuntimeException("Unknown DID"))
      // TODO: Validate keyUsage and revocation
      // we haven't defined which keys can sign requests, and the model doesn't specify when a key is revoked
      publicKey = didDocument.publicKeys
        .find(_.id == authenticationHeader.keyId)
        .flatMap(_.keyData.ecKeyData)
        .map { data =>
          // TODO: Validate curve, right now we support a single curve
          ECKeys.toPublicKey(x = data.x.toByteArray, y = data.y.toByteArray)
        }
        .getOrElse(throw new RuntimeException("Unknown public key id"))

      // Verify the actual signature
      _ <- Either
        .cond(
          ECSignature.verify(publicKey, request, authenticationHeader.signature),
          (),
          SignatureVerificationError()
        )
        .toFutureEither
    } yield participantId
  }

  override def authenticated[Request <: GeneratedMessage, Response <: GeneratedMessage](
      methodName: String,
      request: Request
  )(
      f: ParticipantId => Future[Response]
  )(implicit ec: ExecutionContext, headersParser: RequestHeadersParser): Future[Response] = {
    {
      headersParser.parseAuthenticationHeader
        .map(authenticate(request.toByteArray, _))
        .map { value =>
          value.map(v => withLogging(methodName, request, v) { f(v) }).successMap(identity)
        }
        .getOrElse {
          logger.error(s"$methodName - missing userId, request = ${request.toProtoString}")
          Future.failed(throw new RuntimeException("Missing userId"))
        }
    }.flatten
  }

  override def public[Request <: GeneratedMessage, Response <: GeneratedMessage](methodName: String, request: Request)(
      f: => Future[Response]
  )(implicit ec: ExecutionContext): Future[Response] = {
    withLogging(methodName, request)(f)
  }
}
