package io.iohk.atala.prism.connector.services

import cats.syntax.either._
import io.iohk.atala.prism.auth.AuthenticatorWithGrpcHeaderParser
import io.iohk.atala.prism.connector.errors.{ConnectorError, ConnectorErrorSupport}
import io.iohk.atala.prism.connector.grpc.ProtoCodecs
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.metrics.RequestMeasureUtil.measureRequestFuture
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.connector_api.{
  ConnectionsStatusRequest,
  ConnectionsStatusResponse,
  ContactConnectionServiceGrpc
}
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class ContactConnectionService(
    connectionsService: ConnectionsService[IOWithTraceIdContext],
    authenticator: AuthenticatorWithGrpcHeaderParser[ParticipantId],
    didWhitelist: Set[DID]
)(implicit
    executionContext: ExecutionContext
) extends ContactConnectionServiceGrpc.ContactConnectionService
    with ConnectorErrorSupport {

  val serviceName = "ContactConnectionService"

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getConnectionStatus(
      request: ConnectionsStatusRequest
  ): Future[ConnectionsStatusResponse] = {
    val methodName = "getConnectionStatus"
    def f(did: DID): Future[ConnectionsStatusResponse] = {
      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "did" -> did)

      connectionsService
        .getConnectionsByConnectionTokens(
          request.connectionTokens.map(TokenString(_)).to(List)
        )
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .map(_.asRight[ConnectorError])
        .toFutureEither
        .wrapAndRegisterExceptions(serviceName, methodName)
        .successMap { contactConnections =>
          ConnectionsStatusResponse(
            connections = contactConnections
              .map(ProtoCodecs.contactConnection2Proto.transform)
          )
        }
    }

    authenticator.whitelistedDid(didWhitelist, methodName, request) { did =>
      measureRequestFuture("contact-connection-service", methodName)(f(did))
    }
  }
}
