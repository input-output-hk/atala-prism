package io.iohk.cvp.intdemo

import io.grpc.stub.StreamObserver
import io.iohk.connector.ConnectorService
import io.iohk.cvp.intdemo.IDServiceImpl.log
import io.iohk.cvp.intdemo.protos.IDServiceGrpc._
import io.iohk.cvp.intdemo.protos._
import io.iohk.prism.protos.connector_api
import monix.execution.Scheduler
import monix.execution.Scheduler.{global => scheduler}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class IDServiceImpl(connectorService: ConnectorService, schedulerPeriod: FiniteDuration)(
    credentialStatusRepository: CredentialStatusRepository
)(
    implicit ec: ExecutionContext
) extends IDService {

  override def getConnectionToken(request: GetConnectionTokenRequest): Future[GetConnectionTokenResponse] = {
    for {
      connectionResponse <- connectorService.generateConnectionToken(connector_api.GenerateConnectionTokenRequest())
      token = connectionResponse.token
      _ <- credentialStatusRepository.merge(token, SubjectStatus.UNCONNECTED.value)
    } yield {
      log.debug(s"Generated new connection token in IDService. request = $request, token = ${connectionResponse.token}")
      GetConnectionTokenResponse(connectionResponse.token)
    }
  }

  override def getSubjectStatus(request: GetSubjectStatusRequest): Future[GetSubjectStatusResponse] = {
    credentialStatusRepository
      .find(request.connectionToken)
      .map(
        maybeToken =>
          maybeToken
            .fold[SubjectStatus](SubjectStatus.UNCONNECTED)(statusValue => SubjectStatus.fromValue(statusValue))
      )
      .map { status =>
        log.debug(
          s"Getting subjectStatus for request Generated new connection token in IDService. request = $request, status = ${status}"
        )
        GetSubjectStatusResponse(status)
      }
  }

  override def getSubjectStatusStream(
      request: GetSubjectStatusRequest,
      responseObserver: io.grpc.stub.StreamObserver[GetSubjectStatusResponse]
  ): Unit = {
    log.debug(
      s"Serving getSubjectStatusStream for request $request."
    )

    schedulerStatusStream(
      request = request,
      responseObserver = responseObserver,
      scheduler = scheduler
    )
  }

  private def schedulerStatusStream(
      request: GetSubjectStatusRequest,
      responseObserver: StreamObserver[GetSubjectStatusResponse],
      scheduler: Scheduler
  ): Unit = {
    scheduler.scheduleOnce(schedulerPeriod) {
      credentialStatusRepository.find(request.connectionToken).foreach { maybeStatus: Option[Int] =>
        val status = maybeStatus.map(SubjectStatus.fromValue).getOrElse(SubjectStatus.UNCONNECTED)

        status match {
          case SubjectStatus.UNCONNECTED =>
            streamUnconnectedResponse(request, status, responseObserver, scheduler)
          case SubjectStatus.CONNECTED =>
            streamConnectedResponse(request, responseObserver)
          case _ =>
            ??? // to implement in next story.
        }
      }
    }
  }

  private def streamUnconnectedResponse(
      request: GetSubjectStatusRequest,
      currentStatus: SubjectStatus,
      responseObserver: StreamObserver[GetSubjectStatusResponse],
      scheduler: Scheduler
  ): Unit = {
    getConnectionStatus(request.connectionToken).onComplete {
      case Success(newStatus) =>
        if (currentStatus != newStatus) { // don't re-issue the same status
          val response = GetSubjectStatusResponse(newStatus)
          log.debug(s"Feeding stream response for request $request, response $response")
          responseObserver.onNext(response)
        }
        schedulerStatusStream(request, responseObserver, scheduler)
      case Failure(exception) =>
        log.info(s"Feeding stream error for request $request, exception $exception")
        responseObserver.onError(exception)
    }
  }

  private def getConnectionStatus(connectionToken: String): Future[SubjectStatus] = {
    connectorService.getConnectionByToken(connector_api.GetConnectionByTokenRequest(connectionToken)).flatMap {
      response =>
        response.connection match {
          case Some(_) =>
            val nextStatus = SubjectStatus.CONNECTED
            credentialStatusRepository
              .merge(connectionToken, nextStatus.value)
              .map(_ => nextStatus)
          case None =>
            Future(SubjectStatus.UNCONNECTED)
        }
    }
  }

  private def streamConnectedResponse(
      request: GetSubjectStatusRequest,
      responseObserver: StreamObserver[GetSubjectStatusResponse]
  ): Unit = {
    val response = GetSubjectStatusResponse(SubjectStatus.CONNECTED)
    log.debug(s"Feeding stream response for request $request, response $response")
    responseObserver.onNext(response)
  }

}

object IDServiceImpl {
  val log = LoggerFactory.getLogger(classOf[IDServiceImpl])
}
