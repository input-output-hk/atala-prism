package io.iohk.atala.prism.node

import _root_.grpc.health.v1.health._
import io.grpc.stub.StreamObserver

import scala.concurrent.Future

/** Simple Health service, for now, the service is healthy if the request is received.
  */
class HealthService extends HealthGrpc.Health {
  def check(request: HealthCheckRequest): Future[HealthCheckResponse] =
    Future.successful(servingResponse)

  def watch(
      request: HealthCheckRequest,
      responseObserver: StreamObserver[HealthCheckResponse]
  ): Unit =
    responseObserver.onNext(servingResponse)

  private val servingResponse =
    HealthCheckResponse().withStatus(HealthCheckResponse.ServingStatus.SERVING)
}
