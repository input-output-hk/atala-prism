package io.iohk.cvp.intdemo

import credential.Credential
import io.grpc.stub.StreamObserver
import io.iohk.connector.model.TokenString
import io.iohk.cvp.intdemo.IntDemoService.log
import io.iohk.cvp.intdemo.protos.SubjectStatus.UNCONNECTED
import io.iohk.cvp.intdemo.protos.{
  GetConnectionTokenRequest,
  GetConnectionTokenResponse,
  GetSubjectStatusRequest,
  GetSubjectStatusResponse
}
import io.iohk.cvp.models.ParticipantId
import monix.execution.Scheduler
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

class IntDemoService[D](
    issuerId: ParticipantId,
    connectorIntegration: ConnectorIntegration,
    intDemoRepository: IntDemoRepository,
    schedulerPeriod: FiniteDuration,
    requiredDataLoader: TokenString => Future[Option[D]],
    getCredential: D => Credential,
    scheduler: Scheduler
)(
    implicit ec: ExecutionContext
) {

  def getConnectionToken(request: GetConnectionTokenRequest): Future[GetConnectionTokenResponse] = {
    for {
      connectionToken <- connectorIntegration.generateConnectionToken(issuerId)
      _ <- intDemoRepository.mergeSubjectStatus(connectionToken, UNCONNECTED)
    } yield {
      log.debug(s"Generated new connection token in IDService. request = $request, token = ${connectionToken}")
      GetConnectionTokenResponse(connectionToken.token)
    }
  }

  def getSubjectStatusStream(
      request: GetSubjectStatusRequest,
      responseObserver: StreamObserver[GetSubjectStatusResponse]
  ): Unit = {

    log.debug(
      s"Serving getSubjectStatusStream for request $request."
    )

    val stateMachine =
      new IntDemoStateMachine(
        requiredDataLoader,
        getCredential,
        connectorIntegration,
        intDemoRepository,
        new TokenString(request.connectionToken),
        issuerId,
        responseObserver,
        scheduler,
        schedulerPeriod
      )

    scheduler.scheduleOnce(schedulerPeriod)(stateMachine.tick())
  }
}

object IntDemoService {
  private val log = LoggerFactory.getLogger(this.getClass)
}
