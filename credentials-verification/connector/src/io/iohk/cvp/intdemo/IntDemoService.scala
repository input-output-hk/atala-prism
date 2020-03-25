package io.iohk.cvp.intdemo

import io.grpc.stub.StreamObserver
import io.iohk.connector.model.{Connection, TokenString}
import io.iohk.cvp.intdemo.IntDemoService.log
import io.iohk.cvp.models.ParticipantId
import io.iohk.prism.intdemo.protos.{intdemo_api, intdemo_models}
import io.iohk.prism.protos.credential_models
import monix.execution.Scheduler
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
    getCredential: D => credential_models.Credential,
    scheduler: Scheduler
)(
    implicit ec: ExecutionContext
) {

  def getConnectionToken(
      request: intdemo_api.GetConnectionTokenRequest
  ): Future[intdemo_api.GetConnectionTokenResponse] = {
    for {
      connectionToken <- connectorIntegration.generateConnectionToken(issuerId)
      _ <- intDemoRepository.mergeSubjectStatus(connectionToken, intdemo_models.SubjectStatus.UNCONNECTED)
    } yield {
      log.debug(s"Generated new connection token in IDService. request = $request, token = ${connectionToken}")
      intdemo_api.GetConnectionTokenResponse(connectionToken.token)
    }
  }

  def getSubjectStatusStream(
      request: intdemo_api.GetSubjectStatusRequest,
      responseObserver: StreamObserver[intdemo_api.GetSubjectStatusResponse]
  ): Unit = {

    log.debug(
      s"Serving getSubjectStatusStream for request $request."
    )

    val stateMachine =
      new IntDemoStateMachine(
        requiredDataLoader = requiredDataLoader,
        getCredential = getCredential,
        proofRequestIssuer = proofRequestIssuer,
        connectorIntegration = connectorIntegration,
        intDemoRepository = intDemoRepository,
        connectionToken = new TokenString(request.connectionToken),
        issuerId = issuerId,
        responseObserver = responseObserver,
        scheduler = scheduler,
        schedulerPeriod = schedulerPeriod
      )

    scheduler.scheduleOnce(schedulerPeriod)(stateMachine.tick())
  }
}

object IntDemoService {
  private val log = LoggerFactory.getLogger(this.getClass)
}
