package io.iohk.atala.prism.intdemo

import cats.effect.unsafe.IORuntime
import io.grpc.stub.StreamObserver
import io.iohk.atala.prism.intdemo.IntDemoService.log
import io.iohk.atala.prism.connector.model.{Connection, TokenString}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.intdemo.protos.{intdemo_api, intdemo_models}
import io.iohk.atala.prism.protos.credential_models
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class IntDemoService[D](
    issuerId: ParticipantId,
    connectorIntegration: ConnectorIntegration,
    intDemoRepository: IntDemoRepository,
    schedulerPeriod: FiniteDuration,
    requiredDataLoader: TokenString => Future[Option[D]],
    proofRequestIssuer: Connection => Future[Unit],
    getCredential: D => credential_models.PlainTextCredential
)(implicit ec: ExecutionContext, runtime: IORuntime) {

  def getConnectionToken(
      request: intdemo_api.GetConnectionTokenRequest
  ): Future[intdemo_api.GetConnectionTokenResponse] = {
    for {
      connectionToken <- connectorIntegration.generateConnectionToken(issuerId)
      _ <- intDemoRepository.mergeSubjectStatus(
        connectionToken,
        intdemo_models.SubjectStatus.UNCONNECTED
      )
    } yield {
      log.debug(
        s"Generated new connection token in IDService. request = $request, token = ${connectionToken}"
      )
      intdemo_api.GetConnectionTokenResponse(connectionToken.token)
    }
  }

  private def getStateMachine(
      connectionToken: TokenString
  ): IntDemoStateMachine[D] = {
    new IntDemoStateMachine(
      requiredDataLoader = requiredDataLoader,
      getCredential = getCredential,
      proofRequestIssuer = proofRequestIssuer,
      connectorIntegration = connectorIntegration,
      intDemoRepository = intDemoRepository,
      connectionToken = connectionToken,
      issuerId = issuerId
    )
  }

  def getSubjectStatus(
      request: intdemo_api.GetSubjectStatusRequest
  ): Future[intdemo_api.GetSubjectStatusResponse] = {
    log.debug(s"Serving getSubjectStatus for request $request.")

    val stateMachine = getStateMachine(new TokenString(request.connectionToken))
    stateMachine
      .getCurrentStatus()
      .map(status => intdemo_api.GetSubjectStatusResponse(status))
  }

  def getSubjectStatusStream(
      request: intdemo_api.GetSubjectStatusRequest,
      responseObserver: StreamObserver[intdemo_api.GetSubjectStatusResponse]
  ): Unit = {
    log.debug(s"Serving getSubjectStatusStream for request $request.")

    val stateMachine = getStateMachine(new TokenString(request.connectionToken))
    stateMachine.streamCurrentStatus(
      responseObserver,
      schedulerPeriod
    )
  }
}

object IntDemoService {
  private val log = LoggerFactory.getLogger(this.getClass)
}
