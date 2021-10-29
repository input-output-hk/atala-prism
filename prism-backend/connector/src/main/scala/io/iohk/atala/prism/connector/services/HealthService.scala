package io.iohk.atala.prism.connector.services

import _root_.grpc.health.v1.health._
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory

import scala.concurrent.Future

/** Simple Health service, for now, the service is healthy if the request is received.
  */
class HealthService extends HealthGrpc.Health {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def check(request: HealthCheckRequest): Future[HealthCheckResponse] = {
    logger.info(s"Replying to health request, service = ${request.service}")
    Future.successful(servingResponse)
  }

  def watch(
      request: HealthCheckRequest,
      responseObserver: StreamObserver[HealthCheckResponse]
  ): Unit = {
    logger.info(s"Watch for health changes, service = ${request.service}")
    responseObserver.onNext(servingResponse)
  }

  private val servingResponse =
    HealthCheckResponse().withStatus(HealthCheckResponse.ServingStatus.SERVING)
}
