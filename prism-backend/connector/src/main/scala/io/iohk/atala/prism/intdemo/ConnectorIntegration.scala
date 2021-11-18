package io.iohk.atala.prism.intdemo

import cats.effect.unsafe.IORuntime
import cats.implicits.catsSyntaxEitherId
import io.iohk.atala.prism.connector.errors.{ConnectorError, ConnectorErrorSupport}
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.services.{ConnectionsService, MessagesService}
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.protos.credential_models.AtalaMessage
import io.iohk.atala.prism.tracing.Tracing._
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

trait ConnectorIntegration {
  def sendCredential(
      senderId: ParticipantId,
      connectionId: ConnectionId,
      credential: credential_models.PlainTextCredential
  ): Future[MessageId]

  def sendProofRequest(
      senderId: ParticipantId,
      connectionId: ConnectionId,
      proofRequest: credential_models.ProofRequest
  ): Future[MessageId]

  def getConnectionByToken(token: TokenString): Future[Option[Connection]]

  def generateConnectionToken(senderId: ParticipantId): Future[TokenString]

  def getMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): Future[Seq[Message]]
}

object ConnectorIntegration {

  class ConnectorIntegrationImpl(
      connectionsService: ConnectionsService[IOWithTraceIdContext],
      messagesService: MessagesService[
        fs2.Stream[IOWithTraceIdContext, *],
        IOWithTraceIdContext
      ]
  )(implicit
      ec: ExecutionContext,
      runtime: IORuntime
  ) extends ConnectorIntegration
      with ConnectorErrorSupport {

    import io.iohk.atala.prism.connector.model.{ConnectionId, TokenString}
    import io.iohk.atala.prism.models.ParticipantId

    val logger: Logger =
      LoggerFactory.getLogger(classOf[ConnectorIntegrationImpl])

    override def sendCredential(
        senderId: ParticipantId,
        connectionId: ConnectionId,
        credential: credential_models.PlainTextCredential
    ): Future[MessageId] =
      sendMessage(
        senderId,
        connectionId,
        AtalaMessage().withPlainCredential(credential).toByteArray
      )

    override def sendProofRequest(
        senderId: ParticipantId,
        connectionId: ConnectionId,
        proofRequest: credential_models.ProofRequest
    ): Future[MessageId] =
      sendMessage(
        senderId,
        connectionId,
        AtalaMessage().withProofRequest(proofRequest).toByteArray
      )

    private def sendMessage(
        senderId: ParticipantId,
        connectionId: ConnectionId,
        message: Array[Byte]
    ): Future[MessageId] = trace { traceId =>
      messagesService
        .insertMessage(senderId, connectionId, message)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .toFuture(toRuntimeException(senderId, connectionId))
    }

    override def getConnectionByToken(
        token: TokenString
    ): Future[Option[Connection]] = trace { traceId =>
      connectionsService
        .getConnectionByToken(token)
        .run(traceId)
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
        .toFuture(toRuntimeException)
    }

    override def generateConnectionToken(
        senderId: ParticipantId
    ): Future[TokenString] = trace { traceId =>
      connectionsService
        .generateTokens(senderId, tokensCount = 1)
        .run(traceId)
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
        .toFuture(toRuntimeException)
        .flatMap(
          _.headOption
            .map(Future.successful)
            .getOrElse(
              Future.failed(
                new RuntimeException(
                  "Connection service returned empty connection tokens list"
                )
              )
            )
        )
    }

    override def getMessages(
        recipientId: ParticipantId,
        connectionId: ConnectionId
    ): Future[Seq[Message]] = trace { traceId =>
      messagesService
        .getConnectionMessages(recipientId, connectionId)
        .run(traceId)
        .unsafeToFuture()
    }

    private def toRuntimeException(
        senderId: ParticipantId,
        connectionId: ConnectionId
    ): Any => RuntimeException =
      _ =>
        new RuntimeException(
          s"Failed to send credential for senderId = $senderId, connectionId = $connectionId."
        )

    private def toRuntimeException: ConnectorError => RuntimeException =
      error =>
        new RuntimeException(
          s"Failed to get connection due to connector error: '${error.toStatus.getDescription}'."
        )
  }
}
