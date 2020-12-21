package io.iohk.atala.prism.connector.services

import io.iohk.atala.prism.auth.AuthenticatorWithGrpcHeaderParser
import io.iohk.atala.prism.connector.errors.ConnectorErrorSupport
import io.iohk.atala.prism.connector.grpc.ProtoCodecs
import io.iohk.atala.prism.connector.services.ConnectionsService
import io.iohk.atala.prism.errors.LoggingContext
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
    authenticator: AuthenticatorWithGrpcHeaderParser[ParticipantId]
)(implicit
    executionContext: ExecutionContext
) extends ContactConnectionServiceGrpc.ContactConnectionService
    with ConnectorErrorSupport {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getConnectionStatus(request: ConnectionsStatusRequest): Future[ConnectionsStatusResponse] = {
    def f(participantId: ParticipantId): Future[ConnectionsStatusResponse] = {
      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "participantId" -> participantId)

      connectionsService
        .getAcceptorConnections(request.acceptorIds.map(id => ParticipantId(id)).to(List))
        .wrapExceptions
        .successMap { contactConnections =>
          ConnectionsStatusResponse(
            connections = contactConnections.map(ProtoCodecs.contactConnection2Proto.transform)
          )
        }
    }

    authenticator.authenticated("getConnectionStatus", request) { participantId =>
      f(participantId)
    }
  }
}
