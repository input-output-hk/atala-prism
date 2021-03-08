package io.iohk.atala.prism.connector.services

import io.iohk.atala.prism.auth.AuthenticatorWithGrpcHeaderParser
import io.iohk.atala.prism.connector.errors.ConnectorErrorSupport
import io.iohk.atala.prism.connector.grpc.ProtoCodecs
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.connector.services.ConnectionsService
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.connector_api.{
  ConnectionsStatusRequest,
  ConnectionsStatusResponse,
  ContactConnectionServiceGrpc
}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class ContactConnectionService(
    connectionsService: ConnectionsService,
    authenticator: AuthenticatorWithGrpcHeaderParser[ParticipantId],
    didWhitelist: Set[DID]
)(implicit
    executionContext: ExecutionContext
) extends ContactConnectionServiceGrpc.ContactConnectionService
    with ConnectorErrorSupport {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getConnectionStatus(request: ConnectionsStatusRequest): Future[ConnectionsStatusResponse] = {
    def f(did: DID): Future[ConnectionsStatusResponse] = {
      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "did" -> did)

      connectionsService
        .getConnectionsByConnectionTokens(request.connectionTokens.map(TokenString(_)).to(List))
        .wrapExceptions
        .successMap { contactConnections =>
          ConnectionsStatusResponse(
            connections = contactConnections.map(ProtoCodecs.contactConnection2Proto.transform)
          )
        }
    }

    authenticator.whitelistedDid(didWhitelist, "getConnectionStatus", request) { did =>
      f(did)
    }
  }
}
