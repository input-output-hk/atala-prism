package io.iohk.connector

import io.iohk.connector.UserIdInterceptor.{SignatureHeader, getSignatureHeader, participantId}
import io.iohk.connector.errors.{ConnectorError, ErrorSupport, SignatureVerificationError}
import io.iohk.connector.repositories.ConnectionsRepository
import io.iohk.cvp.crypto.ECKeys.toPublicKey
import io.iohk.cvp.crypto.ECSignature
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither._
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait Authenticator {
  def logger: Logger

  def authenticated[Request <: GeneratedMessage, Response <: GeneratedMessage](
      methodName: String,
      request: Request
  )(f: ParticipantId => Future[Response])(implicit ec: ExecutionContext): Future[Response]

  def public[Request <: GeneratedMessage, Response <: GeneratedMessage](methodName: String, request: Request)(
      f: => Future[Response]
  )(implicit ec: ExecutionContext): Future[Response]

}

class SignedRequestsAuthenticator(connectionsRepository: ConnectionsRepository)
    extends Authenticator
    with ErrorSupport {
  override val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def withLogging[Request <: GeneratedMessage, Response <: GeneratedMessage](
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

  def withLogging[Request <: GeneratedMessage, Response <: GeneratedMessage](methodName: String, request: Request)(
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

  private def authenticate(request: Array[Byte], signatureHeader: SignatureHeader)(
      implicit executionContext: ExecutionContext
  ): FutureEither[ConnectorError, ParticipantId] = {

    for {
      signature <- Future { Right(signatureHeader.signature) }.toFutureEither
      publicKey <- Future { Right(toPublicKey(signatureHeader.publicKey)) }.toFutureEither
      _ <- Either
        .cond(
          ECSignature.verify(publicKey, request, signature),
          (),
          SignatureVerificationError()
        )
        .toFutureEither
      participantId <- connectionsRepository.getParticipantId(signatureHeader.publicKey)
    } yield participantId

  }

  override def authenticated[Request <: GeneratedMessage, Response <: GeneratedMessage](
      methodName: String,
      request: Request
  )(f: ParticipantId => Future[Response])(implicit ec: ExecutionContext): Future[Response] = {
    {
      Try {
        getSignatureHeader()
          .map(authenticate(request.toByteArray, _))
          .getOrElse(Right(participantId()).toFutureEither)
      } match {
        case Failure(ex) =>
          logger.error(s"$methodName - missing userId, request = ${request.toProtoString}", ex)
          Future.failed(throw new RuntimeException("Missing userId"))
        case Success(value) =>
          value.map(v => withLogging(methodName, request, v) { f(v) }).successMap(identity)
      }
    }.flatten
  }

  override def public[Request <: GeneratedMessage, Response <: GeneratedMessage](methodName: String, request: Request)(
      f: => Future[Response]
  )(implicit ec: ExecutionContext): Future[Response] = {
    withLogging(methodName, request)(f)
  }

}
